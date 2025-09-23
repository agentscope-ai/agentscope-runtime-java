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
package io.agentscope.runtime.engine.deployer.a2a;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GraphAgentExecutor implements AgentExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphAgentExecutor.class);

    private final BaseAgent ExecuteAgent;
    private final MemoryService memoryService;
    private final SessionHistoryService sessionHistoryService;

    public GraphAgentExecutor(BaseAgent ExecuteAgent, MemoryService memoryService, SessionHistoryService sessionHistoryService) {
        this.ExecuteAgent = ExecuteAgent;
        this.memoryService = memoryService;
        this.sessionHistoryService = sessionHistoryService;
    }

    private Task new_task(Message request) {
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
        try {
            Message message = context.getParams().message();
            // Prepare/create session
            String sessionId = getSessionId(message);
            String userId = getUserId(message);
            Session session = sessionHistoryService.getSession(userId, sessionId).join().orElse(sessionHistoryService.createSession(userId, java.util.Optional.of(sessionId)).join());

            // Record user input to session and long-term memory
            io.agentscope.runtime.engine.memory.model.Message userMsg = buildTextMessage(getTextFromMessageParts(message));
            sessionHistoryService.appendMessage(session, java.util.List.of(userMsg));
            memoryService.addMemory(userId, java.util.List.of(userMsg), java.util.Optional.of(sessionId));

            String inputText = getTextFromMessageParts(message);
            // Retrieve relevant memories and concatenate to input prefix
            java.util.List<io.agentscope.runtime.engine.memory.model.Message> retrieved = memoryService.searchMemory(
                    userId,
                    java.util.List.of(userMsg),
                    java.util.Optional.of(java.util.Map.of("top_k", 5))
            ).join();
            String retrievedText = formatRetrievedText(retrieved);
            String finalInput = retrievedText.isEmpty() ? inputText : (retrievedText + "\n\n" + inputText);
            Map<String, Object> input = Map.of("input", finalInput, "userId", userId, "sessionId", sessionId);

            Flux<NodeOutput> resultFlux = ExecuteAgent.stream(input);

            Task task = context.getTask();
            if (task == null) {
                task = new_task(context.getMessage());
                eventQueue.enqueueEvent(task);
            }
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);

            StringBuilder accumulatedOutput = new StringBuilder();
            try {
                processStreamingOutput(resultFlux, taskUpdater, accumulatedOutput, sessionId, userId);
            } catch (Exception e) {
                LOGGER.error("Error processing streaming output", e);
                taskUpdater.startWork(taskUpdater.newAgentMessage(
                        List.of(new TextPart("Error processing streaming output: " + e.getMessage())),
                        Map.of()
                ));
                taskUpdater.complete();
            }

            // Write final assistant output to session and long-term memory
            if (!accumulatedOutput.isEmpty()) {
                io.agentscope.runtime.engine.memory.model.Message assistantMsg = buildTextMessage(accumulatedOutput.toString());
                sessionHistoryService.appendMessage(session, java.util.List.of(assistantMsg));
                memoryService.addMemory(userId, java.util.List.of(assistantMsg), java.util.Optional.of(sessionId));
            }

        } catch (Exception e) {
            LOGGER.error("Agent execution failed", e);
            eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
        }
    }

    private String getUserId(Message message) {
        if (message.getMetadata() != null && message.getMetadata().containsKey("userId")) {
            return String.valueOf(message.getMetadata().get("userId"));
        }
        return "default_user";
    }

    private String getSessionId(Message message) {
        if (message.getMetadata() != null && message.getMetadata().containsKey("sessionId")) {
            return String.valueOf(message.getMetadata().get("sessionId"));
        }
        return "default_session";
    }

    /**
     * Process streaming output data
     */
    private void processStreamingOutput(Flux<NodeOutput> resultFlux, TaskUpdater taskUpdater, StringBuilder accumulatedOutput, String sessionId, String userId) {
        try {
            resultFlux
                .doOnNext(output -> {
                    try {
                        LOGGER.info("Processing output: {}", output);

                        String content;
                        if (output instanceof StreamingOutput streamingOutput) {
                            content = streamingOutput.chunk();
                            if (content != null && !content.isEmpty()) {
                                taskUpdater.startWork(taskUpdater.newAgentMessage(
                                        List.of(new TextPart(content)),
                                        Map.of(
                                                "userId", userId,
                                                "sessionId", sessionId
                                        )
                                ));
                                accumulatedOutput.append(content);
                            }
                        } else {
                            Map<String, Object> stateData = output.state().data();
                            if (stateData.containsKey("output")) {
                                content = String.valueOf(stateData.get("output"));
                                if (content != null && !content.isEmpty()) {
                                    taskUpdater.startWork(taskUpdater.newAgentMessage(
                                            List.of(new TextPart(content)),
                                            Map.of()
                                    ));
                                    accumulatedOutput.append(content);
                                }
                            } else {
                                LOGGER.debug("No output field found in NodeOutput, available fields: {}", stateData.keySet());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while processing single output", e);
                    }
                })
                .doOnComplete(taskUpdater::complete)
                .doOnError(e -> {
                    LOGGER.error("Error occurred during streaming processing", e);
                    taskUpdater.startWork(taskUpdater.newAgentMessage(
                            List.of(new TextPart("Streaming error occurred: " + e.getMessage())),
                            Map.of()
                    ));
                    taskUpdater.complete();
                })
                .subscribe();

        } catch (Exception e) {
            LOGGER.error("Error in processStreamingOutput", e);
            taskUpdater.startWork(taskUpdater.newAgentMessage(
                    List.of(new TextPart("Error processing streaming output: " + e.getMessage())),
                    Map.of()
            ));
            taskUpdater.complete();
        }
    }

    private static String getTextFromMessageParts(Message message) {
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
