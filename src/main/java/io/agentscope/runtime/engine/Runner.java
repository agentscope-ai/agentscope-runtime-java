package io.agentscope.runtime.engine;

import reactor.core.publisher.Flux;
import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.schemas.context.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Runner implements AutoCloseable {

    private static Agent agent;
    private static ContextManager contextManager;

    public Runner(Agent agent, ContextManager contextManager) {
        Runner.agent = agent;
        Runner.contextManager = contextManager;
    }

    public Runner() {
    }

    public void registerAgent(Agent agent) {
        Runner.agent = agent;
    }

    public void registerContextManager(ContextManager contextManager) {
        Runner.contextManager = contextManager;
    }

    public static Flux<Event> streamQuery(AgentRequest request) {
        System.out.println(agent.getName());
        return Flux.create(sink -> {
            try {
                // Get or create Session
                io.agentscope.runtime.engine.memory.model.Session memorySession = getOrCreateSession(request.getUserId(), request.getSessionId());

                Session session = new Session();
                session.setId(memorySession.getId());
                session.setUserId(memorySession.getUserId());
                // Convert history message types
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
                context.setAgent(agent);

                if (request.getInput() != null && !request.getInput().isEmpty()) {
                    context.setCurrentMessages(request.getInput());
                }

                CompletableFuture<Flux<Event>> agentFuture = agent.runAsync(context);

                agentFuture.thenAccept(eventFlux -> {
                    StringBuilder aiResponse = new StringBuilder();
                    eventFlux.subscribe(
                            event -> {
                                sink.next(event);
                                // Collect AI response content
                                if (event instanceof io.agentscope.runtime.engine.schemas.agent.Message message) {
                                    if (io.agentscope.runtime.engine.memory.model.MessageType.MESSAGE.name().equals(message.getType()) &&
                                            "completed".equals(message.getStatus())) {
                                        if (message.getContent() != null && !message.getContent().isEmpty()) {
                                            io.agentscope.runtime.engine.schemas.agent.Content content = message.getContent().get(0);
                                            if (content instanceof io.agentscope.runtime.engine.schemas.agent.TextContent textContent) {
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
    private static String extractCleanText(String text) {
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
    private static io.agentscope.runtime.engine.memory.model.Session getOrCreateSession(String userId, String sessionId) {
        try {
            return contextManager.composeSession(userId, sessionId).join();
        } catch (Exception e) {
            // If the Session does not exist, create it through ContextManager
            try {
                return contextManager.getSessionHistoryService().createSession(userId, Optional.of(sessionId)).join();
            } catch (Exception ex) {
                // If creation fails, return a temporary Session
                return new io.agentscope.runtime.engine.memory.model.Session(sessionId, userId, new ArrayList<>());
            }
        }
    }

    /**
     * Save conversation history to ContextManager
     */
    private static void saveConversationHistory(Context context, String aiResponse) {
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
            contextManager.append(memorySession, messagesToSave).join();

        } catch (Exception e) {
            // Log the error but do not interrupt the process
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
        } catch (Exception e) {
        }
    }
}