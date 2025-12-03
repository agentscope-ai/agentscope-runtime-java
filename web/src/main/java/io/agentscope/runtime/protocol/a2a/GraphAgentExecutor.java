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
package io.agentscope.runtime.protocol.a2a;

import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;

import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.engine.schemas.Content;
import io.agentscope.runtime.engine.schemas.Event;
import io.agentscope.runtime.engine.schemas.MessageType;
import io.agentscope.runtime.engine.schemas.TextContent;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import io.agentscope.runtime.engine.schemas.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class GraphAgentExecutor implements AgentExecutor {
    private final Runner runner;

    private final Map<String, Subscription> subscriptions;

    Logger logger = Logger.getLogger(GraphAgentExecutor.class.getName());

    public GraphAgentExecutor(Runner runner) {
        this.runner = runner;
        this.subscriptions = new ConcurrentHashMap<>();
    }

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
        agentMessage.setType(MessageType.MESSAGE);
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
            Flux<Event> resultFlux = runner.streamQuery(agentRequest);
            Task task = context.getTask();
            if (task == null) {
                task = new_task(context.getMessage());
                eventQueue.enqueueEvent(task);
                logger.info("Created new task: " + task.getId());
            } else {
                logger.info("Using existing task: " + task.getId());
            }
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);

            StringBuilder accumulatedOutput = new StringBuilder();
            try {
                logger.info("Starting streaming output processing");
                processStreamingOutput(resultFlux, taskUpdater, accumulatedOutput);
                logger.info("Streaming output processing completed. Total output length: " + accumulatedOutput.length());
            } catch (Exception e) {
                logger.severe("Error processing streaming output" + e.getMessage());
                taskUpdater.startWork(taskUpdater.newAgentMessage(
                        List.of(new TextPart("Error processing streaming output: " + e.getMessage())),
                        Map.of()
                ));
                taskUpdater.complete();
            }

            // No memory persistence in function-only mode
            logger.info("Agent execution completed successfully");

        } catch (Exception e) {
            logger.severe("Agent execution failed" + e.getMessage());
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
        String artifactId = UUID.randomUUID().toString();
        AtomicBoolean isFirstArtifact = new AtomicBoolean(true);
        try {
            resultFlux
                    .doOnSubscribe(s -> {
                        logger.info("Subscribed to executeFunction result stream");
                        taskUpdater.startWork();
                        subscriptions.put(taskUpdater.getTaskId(), s);
                    })
                    .doOnNext(output -> {
                        try {
                            if (output instanceof Message m) {
                                List<Content> contents = m.getContent();
                                if (contents != null && !contents.isEmpty() && contents.get(0) instanceof TextContent text) {
                                    String content = text.getText();
                                    Map<String, Object> metaData = new HashMap<>();
                                    if (Objects.equals(m.getType(), MessageType.FUNCTION_CALL)) {
                                        metaData.put("type", "toolCall");
                                    } else if (Objects.equals(m.getType(), MessageType.FUNCTION_CALL_OUTPUT)) {
                                        metaData.put("type", "toolResponse");
                                    }
                                    else{
                                        metaData.put("type","chunk");
                                    }
                                    if (content != null && !content.isEmpty()) {

                                        taskUpdater.addArtifact(
                                                List.of(new TextPart(content)),
                                                artifactId,
                                                "agent-response",
                                                metaData,
                                                !isFirstArtifact.getAndSet(false),
                                                false
                                        );
                                        accumulatedOutput.append(content);
                                        logger.info("Appended content chunk (" + content.length() + " chars), total so far: "
                                                + accumulatedOutput.length());
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    })
                    .doOnComplete(() -> {
                        logger.info("Stream processing completed successfully");
                        io.a2a.spec.Message finalMessage = taskUpdater.newAgentMessage(
                                List.of(new TextPart(accumulatedOutput.toString())),
                                Map.of("type", "final_response")
                        );
                        taskUpdater.complete(finalMessage);
                    })
                    .doOnError(e -> {
                        io.a2a.spec.Message errorMessage = taskUpdater.newAgentMessage(
                                List.of(new TextPart("Streaming failed: " + e.getMessage())),
                                Map.of()
                        );
                        taskUpdater.fail(errorMessage);
                    })
                    .doFinally(signal -> {
                        logger.info("Stream terminated: " + signal);
                        subscriptions.remove(taskUpdater.getTaskId());
                    })
                    .blockLast();

        } catch (Exception e) {
            taskUpdater.fail(taskUpdater.newAgentMessage(
                    List.of(new TextPart("Critical error: " + e.getMessage())),
                    Map.of()
            ));
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
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        if (!subscriptions.containsKey(taskUpdater.getTaskId())) {
            throw new RuntimeException("Not found Subscription for Task " + taskUpdater.getTaskId());
        }
        subscriptions.get(taskUpdater.getTaskId()).cancel();
        taskUpdater.cancel();
    }
}
