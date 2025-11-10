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

package io.agentscope.runtime.engine;

import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.schemas.agent.*;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.schemas.context.Session;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Runner {
    private final Agent agent;
    private final ContextManager contextManager;
    private final EnvironmentManager environmentManager;

    // Todo: The current stream property has been completely set to true
    private final boolean stream = true;

    private Runner(RunnerBuilder builder) {
        if(builder.agent == null){
            throw new IllegalArgumentException("Agent has to be initialized!");
        }
        this.contextManager = builder.contextManager == null ? new ContextManager() : builder.contextManager;
        this.agent = builder.agent;
        this.environmentManager = builder.environmentManager;
    }


    public Flux<Event> streamQuery(AgentRequest request) {
        return this.streamQueryInstance(request);
    }

    public Flux<Event> streamQueryInstance(AgentRequest request) {
        return Flux.create(sink -> {
            try {
                // Get or create Session
                io.agentscope.runtime.engine.memory.model.Session memorySession = getOrCreateSession(request.getUserId(), request.getSessionId());

                Session session = new Session();
                session.setId(memorySession.getId());
                session.setUserId(memorySession.getUserId());
                // Convert history message types

                // Todo: Specific implementation of memory module
                List<io.agentscope.runtime.engine.schemas.agent.Message> convertedMessages = new ArrayList<>();
                if (memorySession.getMessages() != null) {
                    for (io.agentscope.runtime.engine.memory.model.Message memoryMsg : memorySession.getMessages()) {
                        io.agentscope.runtime.engine.schemas.agent.Message agentMsg = new io.agentscope.runtime.engine.schemas.agent.Message();
                        agentMsg.setRole(memoryMsg.getType() == io.agentscope.runtime.engine.memory.model.MessageType.USER ? "user" : "assistant");

                        List<io.agentscope.runtime.engine.schemas.agent.Content> content = new ArrayList<>();
                        if (memoryMsg.getContent() != null) {
                            for (io.agentscope.runtime.engine.memory.model.MessageContent msgContent : memoryMsg.getContent()) {
                                io.agentscope.runtime.engine.schemas.agent.TextContent textContent = new io.agentscope.runtime.engine.schemas.agent.TextContent();
                                textContent.setText(msgContent.getText());
                                content.add(textContent);
                            }
                        }
                        agentMsg.setContent(content);
                        convertedMessages.add(agentMsg);
                    }
                }
                session.setMessages(convertedMessages);

                Context context = new Context();
                context.setUserId(request.getUserId());
                context.setSession(session);
                context.setRequest(request);
                context.setAgent(this.agent);
                context.setContextManager(this.contextManager);
                context.setEnvironmentManager(this.environmentManager);

                if (request.getInput() != null && !request.getInput().isEmpty()) {
                    context.setCurrentMessages(request.getInput());
                }

                CompletableFuture<Flux<Event>> agentFuture = this.agent.runAsync(context, this.stream);

                agentFuture.thenAccept(eventFlux -> {
                    StringBuilder aiResponse = new StringBuilder();
                    eventFlux.subscribe(
                            event -> {
                                sink.next(event);
                                // Collect AI response content
                                if (event instanceof Message message) {
                                    if (MessageType.MESSAGE.name().equals(message.getType()) &&
                                            "completed".equals(message.getStatus())) {
                                        if (message.getContent() != null && !message.getContent().isEmpty()) {
                                            Content content = message.getContent().get(0);
                                            if (content instanceof TextContent textContent) {
                                                String text = textContent.getText();
                                                // Extract plain text content, removing ChatResponse wrapper
                                                String cleanText = extractCleanText(text);
                                                aiResponse.append(cleanText);
                                            }
                                        }
                                    }
                                }
                            },
                            sink::error,
                            () -> {
                                // After the conversation is complete, save the history message to ContextManager
                                saveConversationHistory(context, aiResponse.toString());
                                sink.complete();
                            }
                    );
                }).exceptionally(throwable -> {
                    sink.error(throwable);
                    return null;
                });

            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * Extract plain text content from ChatResponse object
     */
    private String extractCleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // If the text contains the complete object information of ChatResponse, try to extract the textContent from it
        if (text.contains("textContent=")) {
            int start = text.indexOf("textContent=") + 12;
            int end = text.indexOf(",", start);
            if (end == -1) end = text.indexOf("}", start);
            if (end == -1) end = text.length();

            String extracted = text.substring(start, end).trim();
            // Remove possible quotes
            if (extracted.startsWith("\"") && extracted.endsWith("\"")) {
                extracted = extracted.substring(1, extracted.length() - 1);
            }
            return extracted;
        }

        // If it is already plain text, return it directly
        return text;
    }

    /**
     * Get or create Session
     */
    private io.agentscope.runtime.engine.memory.model.Session getOrCreateSession(String userId, String sessionId) {
        try {
            return this.contextManager.composeSession(userId, sessionId).join();
        } catch (Exception e) {
            // If the Session does not exist, create it through ContextManager
            try {
                return this.contextManager.getSessionHistoryService().createSession(userId, Optional.of(sessionId)).join();
            } catch (Exception ex) {
                // If creation fails, return a temporary Session
                return new io.agentscope.runtime.engine.memory.model.Session(sessionId, userId, new ArrayList<>());
            }
        }
    }

    /**
     * Save conversation history to ContextManager
     */
    private void saveConversationHistory(Context context, String aiResponse) {
        try {
            // Get current session
            io.agentscope.runtime.engine.memory.model.Session memorySession = getOrCreateSession(context.getUserId(), context.getSession().getUserId());

            // Create a list of messages to be saved
            List<io.agentscope.runtime.engine.memory.model.Message> messagesToSave = new ArrayList<>();

            // Add user messages
            if (context.getCurrentMessages() != null) {
                for (io.agentscope.runtime.engine.schemas.agent.Message userMessage : context.getCurrentMessages()) {
                    io.agentscope.runtime.engine.memory.model.Message memoryMessage = new io.agentscope.runtime.engine.memory.model.Message();
                    memoryMessage.setType(io.agentscope.runtime.engine.memory.model.MessageType.USER);

                    List<io.agentscope.runtime.engine.memory.model.MessageContent> content = new ArrayList<>();
                    if (userMessage.getContent() != null) {
                        for (io.agentscope.runtime.engine.schemas.agent.Content msgContent : userMessage.getContent()) {
                            if (msgContent instanceof io.agentscope.runtime.engine.schemas.agent.TextContent textContent) {
                                content.add(new io.agentscope.runtime.engine.memory.model.MessageContent("text", textContent.getText()));
                            }
                        }
                    }
                    memoryMessage.setContent(content);
                    messagesToSave.add(memoryMessage);
                }
            }

            // Add AI reply message
            if (aiResponse != null && !aiResponse.isEmpty()) {
                io.agentscope.runtime.engine.memory.model.Message aiMessage = new io.agentscope.runtime.engine.memory.model.Message();
                aiMessage.setType(io.agentscope.runtime.engine.memory.model.MessageType.ASSISTANT);

                List<io.agentscope.runtime.engine.memory.model.MessageContent> content = new ArrayList<>();
                content.add(new io.agentscope.runtime.engine.memory.model.MessageContent("text", aiResponse));
                aiMessage.setContent(content);
                messagesToSave.add(aiMessage);
            }

            // Save to ContextManager
            this.contextManager.append(memorySession, messagesToSave).join();

        } catch (Exception e) {
            // Log the error but do not interrupt the process
            e.printStackTrace();
        }
    }

    public EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }

    public ContextManager getContextManager() {
        return contextManager;
    }

    public static RunnerBuilder builder() {
        return new RunnerBuilder();
    }

    public static class RunnerBuilder {
        private Agent agent;
        private ContextManager contextManager;
        private EnvironmentManager environmentManager;

        public RunnerBuilder agent(Agent agent) {
            this.agent = agent;
            return this;
        }

        public RunnerBuilder contextManager(ContextManager contextManager) {
            this.contextManager = contextManager;
            return this;
        }

        public RunnerBuilder environmentManager(EnvironmentManager environmentManager) {
            this.environmentManager = environmentManager;
            return this;
        }

        public Runner build() {
            return new Runner(this);
        }
    }
}