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
package io.agentscope.runtime.autoconfig.deployer.a2a;

import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.memory.model.MessageContent;
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

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@ConditionalOnBean(name = "agentRequestStreamQueryFunction")
public class GraphAgentExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphAgentExecutor.class);

    private final Function<AgentRequest, reactor.core.publisher.Flux<Event>> executeFunction;

    public GraphAgentExecutor(@Qualifier("agentRequestStreamQueryFunction") Function<AgentRequest, reactor.core.publisher.Flux<Event>> executeFunction) {
        this.executeFunction = executeFunction;
    }

    private Task new_task(io.a2a.spec.Message request) {
        String context_id_str = request.getContextId();
        if (context_id_str == null || context_id_str.isEmpty()) {
            context_id_str = java.util.UUID.randomUUID().toString();
        }
        String id = java.util.UUID.randomUUID().toString();
        if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
            id = request.getTaskId();
        }
        return new Task(id, context_id_str, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        LOGGER.info("Starting agent execution for context: {}", context.getTask() != null ? context.getTask().getId() : "new");
        try {
            io.a2a.spec.Message message = context.getParams().message();
            String inputText = getTextFromMessageParts(message);
            String finalInput = inputText;
            LOGGER.debug("Processing input text: {}", finalInput);
            
            // Build AgentRequest from incoming message
            AgentRequest agentRequest = new AgentRequest();
            Message agentMessage = new Message();
            agentMessage.setRole("user");
            TextContent tc = new TextContent();
            tc.setText(finalInput);
            agentMessage.setContent(java.util.List.of(tc));
            agentRequest.setInput(java.util.List.of(agentMessage));
            
            LOGGER.info("Applying executeFunction to agent request");
            reactor.core.publisher.Flux<Event> resultFlux = executeFunction.apply(agentRequest);

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

    private void handleEvent(Event event) {
        if (event instanceof Message) {
            Message message = (Message) event;
            System.out.println("Event - Type: " + message.getType() +
                    ", Role: " + message.getRole() +
                    ", Status: " + message.getStatus());

            if (message.getContent() != null && !message.getContent().isEmpty()) {
                TextContent content = (TextContent) message.getContent().get(0);
                System.out.println("Content: " + content.getText());
            }
        } else {
            System.out.println("Received event: " + event.getClass().getSimpleName());
        }
    }

    /**
     * Process streaming output data
     */
    private void processStreamingOutput(reactor.core.publisher.Flux<Event> resultFlux, TaskUpdater taskUpdater, StringBuilder accumulatedOutput) {
        try {
            // 使用 blockLast() 来确保同步等待流式处理完成
            resultFlux
                    .doOnSubscribe(s -> LOGGER.info("Subscribed to executeFunction result stream"))
                    .doOnNext(output -> {
                        try {
                            System.out.println("Processing output: " + output);

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

    private static String getTextFromMessageParts(io.a2a.spec.Message message) {
        StringBuilder sb = new StringBuilder();
        for (Part<?> each : message.getParts()) {
            if (Part.Kind.TEXT.equals(each.getKind())) {
                sb.append(((TextPart) each).getText()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static io.agentscope.runtime.engine.memory.model.Message buildTextMessage(String text) {
        MessageContent content = new MessageContent("text", text);
        return new io.agentscope.runtime.engine.memory.model.Message(MessageType.MESSAGE, java.util.List.of(content));
    }

    private static String formatRetrievedText(java.util.List<io.agentscope.runtime.engine.memory.model.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[Retrieved Memory]\n");
        int idx = 1;
        for (io.agentscope.runtime.engine.memory.model.Message m : messages) {
            String text = extractTextFromMessage(m);
            if (!text.isEmpty()) {
                sb.append(idx++).append('.').append(' ').append(text).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private static String extractTextFromMessage(io.agentscope.runtime.engine.memory.model.Message message) {
        if (message == null || message.getContent() == null) {
            return "";
        }
        for (MessageContent content : message.getContent()) {
            if ("text".equals(content.getType())) {
                return content.getText();
            }
        }
        return "";
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    }
}
