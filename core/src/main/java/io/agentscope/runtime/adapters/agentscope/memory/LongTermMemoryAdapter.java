package io.agentscope.runtime.adapters.agentscope.memory;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.adapters.agentscope.AgentScopeMessageAdapter;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AgentScope Long Term Memory implementation based on MemoryService.
 *
 * <p>This class stores messages in an underlying MemoryService instance,
 * matching the Python version's AgentScopeLongTermMemory implementation.
 *
 * <p>This adapter bridges AgentScope Java framework's LongTermMemory interface
 * with the runtime's MemoryService, allowing agents to use runtime-backed
 * long-term memory storage.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Create memory service (assumed to exist in runtime)
 * MemoryService service = new InMemoryMemoryService();
 *
 * // Create long-term memory adapter
 * AgentScopeLongTermMemoryAdapter longTermMemory =
 *     new AgentScopeLongTermMemoryAdapter(service, "user_123", "session_456");
 *
 * // Use in ReActAgent
 * ReActAgent agent = ReActAgent.builder()
 *     .name("Assistant")
 *     .model(model)
 *     .longTermMemory(longTermMemory)
 *     .longTermMemoryMode(LongTermMemoryMode.BOTH)
 *     .build();
 * }</pre>
 *
 * @see LongTermMemory
 * @see AgentScopeMessageAdapter
 */
public class LongTermMemoryAdapter implements LongTermMemory {
    private static final Logger logger = LoggerFactory.getLogger(LongTermMemoryAdapter.class);

    private final MemoryService service;
    private final String userId;
    private final String sessionId;
    private final AgentScopeMessageAdapter messageAdapter;

    /**
     * Creates a new AgentScopeLongTermMemoryAdapter.
     *
     * @param service The backend memory service
     * @param userId The user ID linked to this memory
     * @param sessionId The session ID linked to this memory
     */
    public LongTermMemoryAdapter(
            MemoryService service,
            String userId,
            String sessionId) {
        this.service = service;
        this.userId = userId;
        this.sessionId = sessionId;
        this.messageAdapter = new AgentScopeMessageAdapter();
    }

    @Override
    public Mono<Void> record(List<Msg> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return Mono.empty();
        }

        // Filter out null messages
        List<Msg> filteredMsgs = msgs.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (filteredMsgs.isEmpty()) {
            return Mono.empty();
        }

        // Convert AgentScope Msgs to runtime Messages
        List<Message> runtimeMessages = messageAdapter.frameworkMsgToMessage(filteredMsgs);

        // Add to memory service
        return Mono.fromFuture(
                service.addMemory(userId, runtimeMessages, Optional.ofNullable(sessionId))
        ).then();
    }

    @Override
    public Mono<String> retrieve(Msg msg) {
        return retrieve(msg, 5);
    }

    /**
     * Retrieve memory with a specified limit.
     * This matches the Python version's retrieve method signature.
     *
     * @param msg The message to search for in the memory
     * @param limit The maximum number of memories to retrieve per search
     * @return Mono containing the retrieved memory as a string
     */
    public Mono<String> retrieve(Msg msg, int limit) {
        // Handle null message - build a default message
        if (msg == null) {
            msg = Msg.builder()
                    .name("assistant")
                    .content(List.of(TextBlock.builder()
                            .text("")
                            .build()))
                    .role(io.agentscope.core.message.MsgRole.ASSISTANT)
                    .build();
        }

        // Handle single message (Java interface only accepts Msg, not List)
        List<Msg> queryMsgs = List.of(msg);

        // Search memory for each message
        List<CompletableFuture<List<Message>>> searchFutures = queryMsgs.stream()
                .map(queryMsg -> {
                    List<Message> queryMessages = messageAdapter.frameworkMsgToMessage(queryMsg);
                    Map<String, Object> filters = Map.of("top_k", limit);
                    return service.searchMemory(userId, queryMessages, Optional.of(filters));
                })
                .collect(Collectors.toList());

        // Wait for all searches to complete
        return Mono.fromFuture(
                CompletableFuture.allOf(
                        searchFutures.toArray(new CompletableFuture[0])
                ).thenApply(v -> {
                    // Process results - convert Messages to readable format
                    List<String> processedResults = new ArrayList<>();
                    for (CompletableFuture<List<Message>> future : searchFutures) {
                        try {
                            List<Message> result = future.join();
                            String resultText = formatMessagesAsString(result);
                            processedResults.add(resultText);
                        } catch (Exception e) {
                            logger.error("Error retrieving memory", e);
                            processedResults.add("Error: " + e.getMessage());
                        }
                    }
                    return String.join("\n", processedResults);
                })
        );
    }

    /**
     * Record important information to memory (tool function).
     *
     * <p>This method is designed to be used as a tool function that agents can call
     * to record important information. The target content should be specific and concise,
     * e.g., who, when, where, do what, why, how, etc.
     *
     * @param thinking The thinking and reasoning about what to record
     * @param content The content to remember, as a list of strings
     * @return ToolResultBlock containing the result
     */
    public Mono<ToolResultBlock> recordToMemory(String thinking, List<String> content) {
        try {
            // Build AgentScope messages
            List<io.agentscope.core.message.ContentBlock> blocks = new ArrayList<>();

            // Add thinking block
            blocks.add(ThinkingBlock.builder()
                    .thinking(thinking)
                    .build());

            // Add text blocks
            for (String cnt : content) {
                blocks.add(TextBlock.builder()
                        .text(cnt)
                        .build());
            }

            Msg msg = Msg.builder()
                    .name("assistant")
                    .content(blocks)
                    .role(io.agentscope.core.message.MsgRole.ASSISTANT)
                    .build();

            // Record to memory
            return record(List.of(msg))
                    .then(Mono.just(ToolResultBlock.text("Successfully recorded content to memory")));
        } catch (Exception e) {
            logger.error("Error recording content to memory", e);
            return Mono.just(ToolResultBlock.text(
                    "Error recording content to memory: " + e.getMessage()));
        }
    }

    /**
     * Retrieve memory based on keywords (tool function).
     *
     * <p>This method is designed to be used as a tool function that agents can call
     * to retrieve memories based on keywords.
     *
     * @param keywords Concise search cues - such as a person's name, a specific date,
     *                 a location, or a short description of the memory you want to retrieve.
     *                 Each keyword is executed as an independent query against the memory store.
     * @param limit The maximum number of memories to retrieve per search
     * @return ToolResultBlock containing the retrieved memories
     */
    public Mono<ToolResultBlock> retrieveFromMemory(List<String> keywords, int limit) {
        try {
            // Build query messages from keywords
            List<Msg> queryMsgs = new ArrayList<>();
            for (String keyword : keywords) {
                Msg queryMsg = Msg.builder()
                        .name("assistant")
                        .content(List.of(TextBlock.builder()
                                .text(keyword)
                                .build()))
                        .role(io.agentscope.core.message.MsgRole.ASSISTANT)
                        .build();
                queryMsgs.add(queryMsg);
            }

            // Retrieve memories
            List<CompletableFuture<List<Message>>> searchFutures = queryMsgs.stream()
                    .map(queryMsg -> {
                        List<Message> queryMessages = messageAdapter.frameworkMsgToMessage(queryMsg);
                        Map<String, Object> filters = Map.of("top_k", limit);
                        return service.searchMemory(userId, queryMessages, Optional.of(filters));
                    })
                    .collect(Collectors.toList());

            return Mono.fromFuture(
                    CompletableFuture.allOf(
                            searchFutures.toArray(new CompletableFuture[0])
                    ).thenApply(v -> {
                        List<String> results = new ArrayList<>();
                        for (CompletableFuture<List<Message>> future : searchFutures) {
                            try {
                                List<Message> result = future.join();
                                String resultText = formatMessagesAsString(result);
                                results.add(resultText);
                            } catch (Exception e) {
                                logger.error("Error retrieving memory", e);
                                results.add("Error: " + e.getMessage());
                            }
                        }
                        return String.join("\n", results);
                    })
            ).map(ToolResultBlock::text);
        } catch (Exception e) {
            logger.error("Error retrieving memory", e);
            return Mono.just(ToolResultBlock.text(
                    "Error retrieving memory: " + e.getMessage()));
        }
    }

    /**
     * Format a list of runtime Messages as a readable string.
     * Converts Messages back to AgentScope Msg format and extracts text content,
     * matching Python version's behavior of converting results to strings.
     *
     * @param messages List of runtime Message objects
     * @return Formatted string representation of the messages
     */
    private String formatMessagesAsString(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        try {
            // Convert runtime Messages back to AgentScope Msgs
            Object agentscopeMsgs = messageAdapter.messageToFrameworkMsg(messages);
            List<Msg> msgList;

            if (agentscopeMsgs instanceof List) {
                @SuppressWarnings("unchecked")
                List<Msg> list = (List<Msg>) agentscopeMsgs;
                msgList = list;
            } else if (agentscopeMsgs instanceof Msg) {
                msgList = List.of((Msg) agentscopeMsgs);
            } else {
                // Fallback: use Message.toString() if conversion fails
                return messages.stream()
                        .map(Message::toString)
                        .collect(Collectors.joining("\n"));
            }

            // Extract text content from AgentScope Msgs
            return msgList.stream()
                    .map(this::extractTextFromMsg)
                    .filter(text -> text != null && !text.isEmpty())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            logger.warn("Failed to convert Messages to AgentScope format, using toString()", e);
            // Fallback: use Message.toString() if conversion fails
            return messages.stream()
                    .map(Message::toString)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * Extract text content from an AgentScope Msg.
     * Collects text from all TextBlock and ThinkingBlock content blocks.
     *
     * @param msg AgentScope Msg object
     * @return Extracted text content
     */
    private String extractTextFromMsg(Msg msg) {
        if (msg == null || msg.getContent() == null) {
            return "";
        }

        List<String> textParts = new ArrayList<>();
        for (io.agentscope.core.message.ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock) {
                String text = ((TextBlock) block).getText();
                if (text != null && !text.isEmpty()) {
                    textParts.add(text);
                }
            } else if (block instanceof ThinkingBlock) {
                String thinking = ((ThinkingBlock) block).getThinking();
                if (thinking != null && !thinking.isEmpty()) {
                    textParts.add(thinking);
                }
            }
            // Other block types (tool_use, tool_result, etc.) are skipped
            // as they are typically not useful for memory retrieval results
        }

        return String.join(" ", textParts);
    }

}

