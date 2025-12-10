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

package io.agentscope.browser.controller;

import io.agentscope.browser.agent.AgentscopeBrowserUseAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

/**
 * REST controller for browser agent chat endpoints
 */
@RestController
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private static final String USER_ID = "user_1";
    private static final String SESSION_ID = "session_001";  // Using a fixed ID for simplicity

    private final AgentscopeBrowserUseAgent agent;

    public ChatController(AgentscopeBrowserUseAgent agent) {
        this.agent = agent;
    }

    @PostConstruct
    public void init() {
        this.agent.start();
    }

    /**
     * Stream chat completions endpoint (OpenAI-compatible)
     */
    @PostMapping(value = {"/v1/chat/completions", "/chat/completions"},
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatCompletions(@RequestBody Map<String, Object> request) {
        logger.info("Received chat completion request");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");

        if (messages == null || messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No messages provided"));
        }

        // Convert chat messages to agent request format
        List<Msg> convertedMessages = new ArrayList<>();
        Msg msg = Msg.builder().role(MsgRole.USER).textContent(messages.get(messages.size()-1).get("content")).build();
        convertedMessages.add(msg);

        // Create agent request
        AgentRequest agentRequest = new AgentRequest();
        agentRequest.setSessionId(SESSION_ID);
        agentRequest.setUserId(USER_ID);

        return agent.streamQuery(agentRequest, convertedMessages)
                .flatMap(event -> {
                    EventType type = event.getType();
                    boolean isLast = event.isLast() || type == EventType.AGENT_RESULT;
                    String messageType = type != null ? type.name() : null;

					Msg msgGenerated = event.getMessage();
                    if (msgGenerated == null || msgGenerated.getContent() == null || msgGenerated.getContent()
                            .isEmpty()) {
                        return Flux.just(simpleYield("", "content", messageType));
                    }

                    List<ContentBlock> contentList = msgGenerated.getContent();
                    StringBuilder responseBuilder = new StringBuilder();
                    StringBuilder thinkingBuilder = new StringBuilder();


                    String toolCallName = null;
                    for (ContentBlock item : contentList) {
                        if (item instanceof TextBlock textContent && !isLast) {
                            String text = textContent.getText();
                            if (text != null && !text.isEmpty()) {
                                responseBuilder.append(text);
                                System.out.println(text);
                            }
                            messageType = "ASSISTANT";
                        }
                        else if (item instanceof ToolUseBlock toolUseBlock) {
                            responseBuilder.append("Using tool: ").append(toolUseBlock.getName()).append("\n");
                            toolCallName = toolUseBlock.getName();
                            messageType = "TOOL_CALL";
                        } else if (item instanceof ToolResultBlock toolResultBlock) {
                            responseBuilder.append("Tool result: ").append("\n");
                            toolCallName = toolResultBlock.getName();
                            for (ContentBlock toolResultContentBlock : toolResultBlock.getOutput()) {
                                if (toolResultContentBlock instanceof TextBlock toolResultTextBlock) {
                                    responseBuilder.append(toolResultTextBlock.getText());
                                }
                            }
                            responseBuilder.append("\n");
                            messageType = "TOOL_RESPONSE";
                        } else if (item instanceof ThinkingBlock thinkingBlock && !isLast) {
                            String thought = thinkingBlock.getThinking();
                            if (thought != null && !thought.isEmpty()) {
                                thinkingBuilder.append(thought).append("\n");
                            }
                            messageType = "THINKING";
                        }
                    }

                    String response = responseBuilder.toString();
                    if (!response.isEmpty()) {
                        return Flux.just(simpleYield(response, "content", messageType, toolCallName, null, null));
                    }

                    String thinking = thinkingBuilder.toString();
                    if (!thinking.isEmpty()) {
                        return Flux.just(simpleYield(thinking, "content", messageType, toolCallName, null, null));
                    }

                    return Flux.just(simpleYield("", "content", messageType));

                })
                .onErrorResume(error -> {
                    logger.error("Error during chat completion", error);
                    return Flux.just(simpleYield("Error: " + error.getMessage(), "content", null, null, null, null));
                });
    }

    /**
     * Get browser environment info
     */
    @GetMapping("/env_info")
    public ResponseEntity<Map<String, String>> getEnvInfo() {
        logger.info("Received env_info request");

        agent.connect(SESSION_ID, USER_ID);

        String baseUrl = agent.getBaseUrl();
        String runtimeToken = agent.getRuntimeToken();

        if (baseUrl != null && !baseUrl.isEmpty() && runtimeToken != null && !runtimeToken.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("baseUrl", baseUrl);
            response.put("runtimeToken", runtimeToken);
            logger.info("Returning baseUrl: {}, runtimeToken: {}", baseUrl, runtimeToken);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Browser sandbox not available");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String simpleYield(String content, String ctype, String messageType) {
        return simpleYield(content, ctype, messageType, null, null, null);
    }

    /**
     * Format response as SSE (Server-Sent Events) compatible string
     */
    private String simpleYield(String content, String ctype, String messageType, String toolName, String toolId, String toolData) {
        Map<String, Object> response = wrapAsOpenAIResponse(content, content, ctype, messageType, toolName, toolId, toolData);

        try {
            // Use simple JSON formatting
            String json = toJson(response);
            return "data: " + json + "\n\n";
        } catch (Exception e) {
            logger.error("Error formatting response", e);
            return "data: {\"error\": \"" + e.getMessage() + "\"}\n\n";
        }
    }

    /**
     * Wrap content as OpenAI-compatible response
     */
    private Map<String, Object> wrapAsOpenAIResponse(String textContent, String cardContent, String ctype, String messageType, String toolName, String toolId, String toolData) {
        String contentType;

        contentType = switch (ctype) {
            case "think" -> "reasoning_content";
            case "site" -> "site_content";
            default -> "content";
        };

        Map<String, Object> delta = new HashMap<>();
        delta.put(contentType, textContent);
        delta.put("cards", cardContent);
        // Add messageType to delta if present
        if (messageType != null) {
            delta.put("messageType", messageType);
        }
        // Add tool information if present
        if (toolName != null) {
            delta.put("toolName", toolName);
        }
        if (toolId != null) {
            delta.put("toolId", toolId);
        }
        if (toolData != null) {
            if ("TOOL_CALL".equals(messageType)) {
                delta.put("toolInput", toolData);
            } else if ("TOOL_RESPONSE".equals(messageType)) {
                delta.put("toolResult", toolData);
            }
        }

        Map<String, Object> choice = new HashMap<>();
        choice.put("delta", delta);
        choice.put("index", 0);
        choice.put("finish_reason", null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", "chat_" + System.currentTimeMillis());
        response.put("object", "chat.completion.chunk");
        response.put("created", System.currentTimeMillis() / 1000);
        response.put("choices", List.of(choice));

        return response;
    }

    /**
     * Simple JSON serialization
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("\"").append(entry.getKey()).append("\":");
            json.append(toJsonValue(entry.getValue()));
        }

        json.append("}");
        return json.toString();
    }

    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append(toJsonValue(item));
            }
            json.append("]");
            return json.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return toJson(map);
        }
        return "\"" + value.toString() + "\"";
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

