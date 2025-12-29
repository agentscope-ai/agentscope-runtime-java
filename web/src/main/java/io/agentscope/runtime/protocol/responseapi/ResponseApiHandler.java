/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.protocol.responseapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.realtime.*;
import com.openai.models.realtime.RealtimeResponse.Status;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.schemas.*;
import io.agentscope.runtime.protocol.responseapi.model.*;
import io.agentscope.runtime.protocol.responseapi.model.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;

/**
 * Handler for OpenAI Responses API requests.
 */
public class ResponseApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResponseApiHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Runner runner;

    public ResponseApiHandler(Runner runner, ResponseApiProtocolConfig config) {
        this.runner = runner;
        // Config is available for future use (e.g., timeout configuration)
    }

    /**
     * Handle chat completion request and return streaming response.
     *
     * @param requestBody the request body containing messages
     * @return Flux of ResponseStreamEvent
     */
    public Flux<RealtimeServerEvent> handleStreamingResponse(ResponseApiRequest requestBody) {
        try {
            Object input = requestBody.getInput();
            if (input == null) {
                return Flux.just(createErrorEvent("No input provided", "invalid_request_error"));
            }

            AgentRequest agentRequest = buildAgentRequest(requestBody);

            Flux<Event> eventFlux = runner.streamQuery(agentRequest);

            return handleStreamingFlux(eventFlux);
        } catch (Exception e) {
            logger.error("Error handling chat completion: {}", e.getMessage());
            return Flux.just(createErrorEvent("Error: " + e.getMessage(), "internal_error"));
        }
    }

    public Flux<RealtimeServerEvent> handleStreamingFlux(Flux<Event> eventFlux) {
        StreamState streamState = new StreamState();
        return Flux.concat(
                Flux.just(RealtimeServerEvent.ofResponseCreated(
                                ResponseCreatedEvent.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .response(
                                                RealtimeResponse.builder()
                                                        .id(streamState.responseId)
                                                        .status(Status.IN_PROGRESS)
                                                        .build()
                                        )
                                        .build()
                        )
                ),
                eventFlux.flatMap(event -> {
                            if (event instanceof Message || event instanceof Content) {
                                return convertMessageToStreamEvent(event, streamState);
                            } else {
                                return Flux.empty();
                            }
                        }).onErrorResume(e -> {
                            logger.error("Streaming error: {}", e.getMessage());
                            return Flux.just(createErrorEvent("Streaming error: " + e.getMessage(), "internal_error"));
                        })
                        .concatWith(Flux.just(
                                RealtimeServerEvent.ofResponseDone(
                                        ResponseDoneEvent.builder()
                                                .eventId(UUID.randomUUID().toString())
                                                .response(
                                                        RealtimeResponse.builder()
                                                                .id(streamState.responseId)
                                                                .status(Status.COMPLETED)
                                                                .build()
                                                )
                                                .build()
                                )
                        ))
        );

    }

    /**
     * Handle non-streaming request and return aggregated response.
     */
    public ResponseApiResponse handleNonStreamingResponse(ResponseApiRequest requestBody) {
        ResponseApiResponse response = new ResponseApiResponse();
        String responseId = "resp_" + UUID.randomUUID();
        Integer created = Math.toIntExact(Instant.now().getEpochSecond());

        response.setId(responseId);
        response.setObject("response");
        response.setCreatedAt(created);
        response.setModel(requestBody.getModel());
        response.setBackground(requestBody.getBackground());
        response.setParallelToolCalls(requestBody.getParallelToolCalls());
        response.setPreviousResponseId(requestBody.getPreviousResponseId());
        response.setInstructions(requestBody.getInstructions());
        response.setMaxOutputTokens(requestBody.getMaxOutputTokens());
        response.setMaxToolCalls(requestBody.getMaxToolCalls());
        response.setMetadata(requestBody.getMetadata());
        response.setPrompt(requestBody.getPrompt());
        response.setPromptCacheKey(requestBody.getPromptCacheKey());
        response.setPromptCacheRetention(requestBody.getPromptCacheRetention());
        response.setReasoning(requestBody.getReasoning());
        response.setSafetyIdentifier(requestBody.getSafetyIdentifier());
        response.setToolChoice(requestBody.getToolChoice());
        if (requestBody.getTopP() != null) {
            response.setTopP(requestBody.getTopP());
        }
        response.setTruncation(requestBody.getTruncation());

        try {
            Object input = requestBody.getInput();
            if (input == null) {
                Error error = new Error();
                error.setMessage("No input provided");
                error.setCode("invalid_request_error");
                response.setStatus("failed");
                response.setError(error);
                return response;
            }

            AgentRequest agentRequest = buildAgentRequest(requestBody);

            List<Event> messages = runner.streamQuery(agentRequest)
                    .collectList()
                    .block();

            if (messages == null) {
                messages = List.of();
            }

            List<OutputMessage> outputItems = buildOutputItems(messages);
            response.setOutput(outputItems);
            response.setStatus("completed");
            response.setUsage(buildUsagePlaceholder());

        } catch (Exception e) {
            logger.error("Error handling non-streaming response: {}", e.getMessage());
            Error error = new Error();
            error.setMessage(e.getMessage());
            error.setCode("internal_error");
            response.setStatus("failed");
            response.setError(error);
        }
        return response;
    }

    private String getUserId(ResponseApiRequest requestBody) {
        if (requestBody.getMetadata() != null && requestBody.getMetadata().containsKey("userId")) {
            return String.valueOf(requestBody.getMetadata().get("userId"));
        }
        return "default_user";
    }

    private String getSessionId(ResponseApiRequest requestBody) {
        if (requestBody.getConversation() != null && requestBody.getConversation().getId() != null) {
            return requestBody.getConversation().getId();
        }
        if (requestBody.getMetadata() != null && requestBody.getMetadata().containsKey("sessionId")) {
            return String.valueOf(requestBody.getMetadata().get("sessionId"));
        }
        return "default_session";
    }

    private AgentRequest buildAgentRequest(ResponseApiRequest requestBody) {
        String inputText = getTextFromMessageParts(requestBody.getInput());
        AgentRequest agentRequest = new AgentRequest();
        Message agentMessage = new Message();
        agentMessage.setType(MessageType.MESSAGE);
        agentMessage.setRole(Role.USER);
        TextContent tc = new TextContent();
        tc.setText(inputText);
        agentMessage.setContent(List.of(tc));
        agentRequest.setUserId(getUserId(requestBody));
        agentRequest.setSessionId(getSessionId(requestBody));
        agentRequest.setInput(List.of(agentMessage));
        return agentRequest;
    }

    private String getTextFromMessageParts(Object input) {
        StringBuilder inputTextBuilder = new StringBuilder();
        if(input instanceof String inputText){
            inputTextBuilder.append(inputText);
        } else if (input instanceof List<?> inputList) {
            for (Object item : inputList) {
                if (item instanceof String text) {
                    if (!text.trim().isBlank()) {
                       inputTextBuilder.append(text.trim());
                    }
                }
            }
        }
        return inputTextBuilder.toString().trim();
    }

    private List<OutputMessage> buildOutputItems(List<Event> events) {
        List<OutputMessage> output = new ArrayList<>();
        OutputMessage item = new OutputMessage();
        item.setType("message");
        item.setId(UUID.randomUUID().toString());
        item.setStatus("completed");
        item.setRole("assistant");
        item.setContent(buildContentPayload(events));
        output.add(item);
        return output;
    }

    private List<ResponseContent> buildContentPayload(List<Event> events) {
        List<ResponseContent> payload = new ArrayList<>();
        StringBuilder accumulatedOutput = new StringBuilder();
        for (Event output : events) {
            if (output instanceof Content) {
                // Todo: only process text content for now, need to handle other content types later
                if (output instanceof TextContent text) {
                    String content = text.getText();
                    accumulatedOutput.append(content);
                    logger.info("Appended content chunk ({} chars), total so far: {}", content.length(), accumulatedOutput.length());
                }
            }
            // Todo: need to know whether the blocking mode should also handle tool calls and responses
            else if (output instanceof Message message) {
                if (message.getType().equals("mcp_call")) {
                    for (Content content : message.getContent()) {
                        if (content instanceof DataContent dataContent) {
                            if (dataContent.getData() == null || !dataContent.getData().containsKey("name") || dataContent.getData().get("name").toString().isEmpty()) {
                                continue;
                            }
                            String toolName = dataContent.getData().get("name").toString();
                            String arguments = dataContent.getData().get("arguments").toString();
                            String callId = dataContent.getData().get("call_id").toString();
                            String textContent = "Calling tool " + toolName + " with arguments: " + arguments + " (call ID: " + callId + ")";
                            Map<String, Object> metaData = new HashMap<>();
                            metaData.put("type", "toolCall");
                            accumulatedOutput.append(textContent);
                            // Todo: Still need to know the exact token usage for tool call
                        }
                    }
                } else if (message.getType().equals("mcp_approval_response")) {
                    for (Content content : message.getContent()) {
                        if (content instanceof DataContent dataContent) {
                            if (dataContent.getData() == null || !dataContent.getData().containsKey("name") || dataContent.getData().get("name").toString().isEmpty()) {
                                continue;
                            }
                            String toolResult = dataContent.getData().get("output").toString();
                            String toolName = dataContent.getData().get("name").toString();
                            String callId = dataContent.getData().get("call_id").toString();
                            String textContent = "Tool " + toolName + " returned result: " + toolResult + " (call ID: " + callId + ")";
                            Map<String, Object> metaData = new HashMap<>();
                            metaData.put("type", "toolResponse");
                            accumulatedOutput.append(textContent);
                            // Todo: Still need to know the exact token usage for tool call
                        }
                    }
                }
            }
        }
        ResponseContent responseContent = new ResponseContent();
        responseContent.setType("output_text");
        responseContent.setText(accumulatedOutput.toString());
        payload.add(responseContent);

        return payload;
    }

    private ResponseUsage buildUsagePlaceholder() {
        // Placeholder: hook actual token accounting if available
        return new ResponseUsage();
    }

    private Flux<RealtimeServerEvent> convertMessageToStreamEvent(Event event, StreamState streamState) {
        List<RealtimeServerEvent> events = new ArrayList<>();

        if (event instanceof Content) {
            if (event instanceof TextContent textContent) {
                String text = textContent.getText();
                ResponseTextDeltaEvent deltaEvent = ResponseTextDeltaEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .responseId(streamState.responseId)
                        .itemId(UUID.randomUUID().toString())
                        .outputIndex(streamState.outputIndex)
                        .contentIndex(streamState.contentIndex)
                        .delta(text)
                        .build();
                streamState.incrementContentIndex();

                events.add(RealtimeServerEvent.ofResponseOutputTextDelta(deltaEvent));
            }
        }
        else if(event instanceof Message message){
            if (message.getType().equals("mcp_call")) {
                for (Content content : message.getContent()) {
                    if (content instanceof DataContent dataContent) {
                        if (dataContent.getData() == null || !dataContent.getData().containsKey("name") || dataContent.getData().get("name").toString().isEmpty()) {
                            continue;
                        }
                        String arguments = dataContent.getData().get("arguments").toString();
                        String callId = dataContent.getData().get("call_id").toString();

                        streamState.incrementOutputIndex();
                        ResponseFunctionCallArgumentsDeltaEvent deltaEvent = ResponseFunctionCallArgumentsDeltaEvent.builder()
                                .eventId(UUID.randomUUID().toString())
                                .responseId(streamState.responseId)
                                .itemId(UUID.randomUUID().toString())
                                .outputIndex(streamState.outputIndex)
                                .delta(arguments)
                                .callId(callId)
                                .build();

                        events.add(RealtimeServerEvent.ofResponseFunctionCallArgumentsDelta(deltaEvent));
                        // Todo: Still need to know the exact token usage for tool call
                    }
                }
            } else if (message.getType().equals("mcp_approval_response")) {
                for (Content content : message.getContent()) {
                    if (content instanceof DataContent dataContent) {
                        if (dataContent.getData() == null || !dataContent.getData().containsKey("name") || dataContent.getData().get("name").toString().isEmpty()) {
                            continue;
                        }
                        String toolResult = dataContent.getData().get("output").toString();
                        String callId = dataContent.getData().get("call_id").toString();

                        ResponseFunctionCallArgumentsDoneEvent deltaEvent = ResponseFunctionCallArgumentsDoneEvent.builder()
                                .eventId(UUID.randomUUID().toString())
                                .responseId(streamState.responseId)
                                .itemId(UUID.randomUUID().toString())
                                .outputIndex(streamState.outputIndex)
                                .arguments(toolResult)
                                .callId(callId)
                                .build();

                        streamState.incrementOutputIndex();
                        events.add(RealtimeServerEvent.ofResponseFunctionCallArgumentsDone(deltaEvent));
                        // Todo: Still need to know the exact token usage for tool call
                    }
                }
            }
        }

        return Flux.fromIterable(events);
    }

    private RealtimeServerEvent createErrorEvent(String errorMessage, String type) {
        RealtimeErrorEvent error = RealtimeErrorEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .error(RealtimeError.builder()
                        .message(errorMessage)
                        .type(type)
                        .build())
                .build();
        return RealtimeServerEvent.ofError(error);
    }

    class StreamState {
        private int outputIndex = 0;
        private int contentIndex = 0;
        private String responseId = "resp_" + UUID.randomUUID();

        public void incrementOutputIndex() {
            this.outputIndex++;
            this.contentIndex = 0; // Reset content index for new output
        }

        public void incrementContentIndex() {
            this.contentIndex++;
        }
    }
}

