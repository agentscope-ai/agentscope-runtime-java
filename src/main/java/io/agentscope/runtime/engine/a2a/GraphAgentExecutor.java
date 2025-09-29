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
package io.agentscope.runtime.engine.a2a;

import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
import io.agentscope.runtime.engine.schemas.agent.*;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public record GraphAgentExecutor(Function<AgentRequest, Flux<Event>> executeFunction) implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphAgentExecutor.class);

    private Task new_task(io.a2a.spec.Message request) {
        String context_id_str = request.getContextId();
        if (context_id_str == null || context_id_str.isEmpty()) {
            context_id_str = UUID.randomUUID().toString();
        }
        String id = UUID.randomUUID().toString();
        if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
            id = request.getTaskId();
        }
        return new Task(id, context_id_str, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
    }

    private AgentRequest buildAgentRequest(RequestContext context) {
        io.a2a.spec.Message message = context.getParams().message();
        String inputText = getTextFromMessageParts(message);
        AgentRequest agentRequest = new AgentRequest();
        Message agentMessage = new Message();
        agentMessage.setRole("user");
        TextContent tc = new TextContent();
        tc.setText(inputText);
        agentMessage.setContent(List.of(tc));
        agentRequest.setUserId(getUserId(message));
        agentRequest.setSessionId(getSessionId(message));
        agentRequest.setInput(List.of(agentMessage));
        return agentRequest;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        try {
            AgentRequest agentRequest = buildAgentRequest(context);
            Flux<Event> resultFlux = executeFunction.apply(agentRequest);
            Task task = context.getTask();
            if (task == null) {
                task = new_task(context.getMessage());
                eventQueue.enqueueEvent(task);
                LOGGER.info("Created new task: {}", task.getId());
            } else {
                LOGGER.info("Using existing task: {}", task.getId());
            }
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);

            StringBuilder accumulatedOutput = new StringBuilder();
            try {
                LOGGER.info("Starting streaming output processing");
                processStreamingOutput(resultFlux, taskUpdater, accumulatedOutput);
                LOGGER.info("Streaming output processing completed. Total output length: {}", accumulatedOutput.length());
            } catch (Exception e) {
                LOGGER.error("Error processing streaming output", e);
                taskUpdater.startWork(taskUpdater.newAgentMessage(
                        List.of(new TextPart("Error processing streaming output: " + e.getMessage())),
                        Map.of()
                ));
                taskUpdater.complete();
            }

            // No memory persistence in function-only mode
            LOGGER.info("Agent execution completed successfully");

        } catch (Exception e) {
            LOGGER.error("Agent execution failed", e);
            eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
        }
    }

    private String getUserId(io.a2a.spec.Message message) {
        if (message.getMetadata() != null && message.getMetadata().containsKey("userId")) {
            return String.valueOf(message.getMetadata().get("userId"));
        }
        return "default_user";
    }

    private String getSessionId(io.a2a.spec.Message message) {
        if (message.getMetadata() != null && message.getMetadata().containsKey("sessionId")) {
            return String.valueOf(message.getMetadata().get("sessionId"));
        }
        return "default_session";
    }

    /**
     * Process streaming output data
     */
    private void processStreamingOutput(Flux<Event> resultFlux, TaskUpdater taskUpdater, StringBuilder accumulatedOutput) {
        try {
            // 使用 blockLast() 来确保同步等待流式处理完成
            resultFlux
                    .doOnSubscribe(s -> LOGGER.info("Subscribed to executeFunction result stream"))
                    .doOnNext(output -> {
                        try {
                            if (output instanceof Message m) {
                                List<Content> contents = m.getContent();
                                if (contents != null && !contents.isEmpty() && contents.get(0) instanceof TextContent text) {
                                    String content = text.getText();
                                    if (content != null && !content.isEmpty()) {
                                        taskUpdater.startWork(taskUpdater.newAgentMessage(
                                                List.of(new TextPart(content)),
                                                Map.of()
                                        ));
                                        accumulatedOutput.append(content);
                                        LOGGER.debug("Appended content chunk ({} chars), total so far {}",
                                                content.length(), accumulatedOutput.length());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error occurred while processing single output", e);
                        }
                    })
                    .doOnComplete(() -> {
                        LOGGER.info("Stream processing completed successfully");
                        taskUpdater.complete();
                    })
                    .doOnError(e -> {
                        LOGGER.error("Error occurred during streaming processing", e);
                        taskUpdater.startWork(taskUpdater.newAgentMessage(
                                List.of(new TextPart("Streaming error occurred: " + e.getMessage())),
                                Map.of()
                        ));
                        taskUpdater.complete();
                    })
                    .doFinally(signal -> LOGGER.info("executeFunction result stream terminated: {}", signal))
                    .blockLast(); // 同步等待流式处理完成

        } catch (Exception e) {
            LOGGER.error("Error in processStreamingOutput", e);
            taskUpdater.startWork(taskUpdater.newAgentMessage(
                    List.of(new TextPart("Error processing streaming output: " + e.getMessage())),
                    Map.of()
            ));
            taskUpdater.complete();
        }
    }

    private String getTextFromMessageParts(io.a2a.spec.Message message) {
        StringBuilder sb = new StringBuilder();
        for (Part<?> each : message.getParts()) {
            if (Part.Kind.TEXT.equals(each.getKind())) {
                sb.append(((TextPart) each).getText()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    }
}
