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
package io.agentscope.runtime.engine.memory.persistence.memory.service;


import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.schemas.message.Message;
import io.agentscope.runtime.engine.schemas.message.TextContent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of memory service
 */
public class InMemoryMemoryService implements MemoryService {
    
    private final Map<String, Map<String, List<Message>>> store = new ConcurrentHashMap<>();
    private static final String DEFAULT_SESSION_ID = "default_session";

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            store.clear();
        });
    }
    
    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            store.clear();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.completedFuture(true);
    }
    
    @Override
    public CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            store.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
            
            String storageKey = sessionId.orElse(DEFAULT_SESSION_ID);
            store.get(userId).computeIfAbsent(storageKey, k -> new ArrayList<>())
                    .addAll(messages);
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            if (!store.containsKey(userId) || messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }
            
            Message lastMessage = messages.get(messages.size() - 1);
            String query = getQueryText(lastMessage);
            if (query == null || query.trim().isEmpty()) {
                return Collections.emptyList();
            }
            
            Set<String> keywords = Arrays.stream(query.toLowerCase().split("\\s+"))
                    .collect(Collectors.toSet());
            
            List<Message> allMessages = store.get(userId).values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            
            List<Message> matchedMessages = allMessages.stream()
                    .filter(msg -> {
                        String content = getQueryText(msg);
                        if (content != null) {
                            String contentLower = content.toLowerCase();
                            return keywords.stream().anyMatch(keyword -> contentLower.contains(keyword));
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
            
            if (filters.isPresent() && filters.get().containsKey("top_k")) {
                Object topKObj = filters.get().get("top_k");
                if (topKObj instanceof Integer) {
                    int topK = (Integer) topKObj;
                    int startIndex = Math.max(0, matchedMessages.size() - topK);
                    return matchedMessages.subList(startIndex, matchedMessages.size());
                }
            }
            
            return matchedMessages;
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            if (!store.containsKey(userId)) {
                return Collections.emptyList();
            }
            
            List<Message> allMessages = store.get(userId).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toList());
            
            if (filters.isPresent()) {
                Map<String, Object> filterMap = filters.get();
                int pageNum = (Integer) filterMap.getOrDefault("page_num", 1);
                int pageSize = (Integer) filterMap.getOrDefault("page_size", 10);
                
                int startIndex = (pageNum - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, allMessages.size());
                
                if (startIndex >= allMessages.size()) {
                    return Collections.emptyList();
                }
                
                return allMessages.subList(startIndex, endIndex);
            }
            
            return allMessages;
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            if (!store.containsKey(userId)) {
                return;
            }
            
            if (sessionId.isPresent()) {
                store.get(userId).remove(sessionId.get());
            } else {
                store.remove(userId);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            return new ArrayList<>(store.keySet());
        });
    }
    
    /**
     * Extract query text from message
     *
     * @param message Message object
     * @return Query text, or null if it cannot be extracted
     */
    private String getQueryText(Message message) {
        if (message == null || message.getContent() == null) {
            return null;
        }

        // Todo: TEST ME
        return message.getContent().stream()
                .filter(content -> "text".equals(content.getType()))
                .filter(content -> content instanceof TextContent)
                .map(content -> ((TextContent) content).getText())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }
    
}
