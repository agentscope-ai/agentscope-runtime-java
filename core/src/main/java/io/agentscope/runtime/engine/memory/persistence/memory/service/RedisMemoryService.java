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

import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Redis-based memory service implementation
 */
public class RedisMemoryService implements MemoryService {
    Logger logger = Logger.getLogger(RedisMemoryService.class.getName());

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String DEFAULT_SESSION_ID = "default_session";

    public RedisMemoryService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                return "PONG".equals(pong);
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    private String getUserKey(String userId) {
        return "user_memory:" + userId;
    }
    
    private String serialize(List<Message> messages) throws JsonProcessingException {
        return objectMapper.writeValueAsString(messages);
    }
    
    private List<Message> deserialize(String messagesJson) throws JsonProcessingException {
        if (messagesJson == null || messagesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});
    }
    
    @Override
    public CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = getUserKey(userId);
                String field = sessionId.orElse(DEFAULT_SESSION_ID);
                
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                String existingJson = hashOps.get(key, field);
                List<Message> existingMessages = deserialize(existingJson);
                
                List<Message> allMessages = new ArrayList<>(existingMessages);
                allMessages.addAll(messages);
                
                hashOps.put(key, field, serialize(allMessages));
            } catch (Exception e) {
                throw new RuntimeException("Failed to add memory to Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = getUserKey(userId);
                if (messages == null || messages.isEmpty()) {
                    return Collections.emptyList();
                }
                
                Message lastMessage = messages.get(messages.size() - 1);
                String query = getQueryText(lastMessage);
                if (query == null || query.trim().isEmpty()) {
                    return Collections.emptyList();
                }
                
                Set<String> keywords = Arrays.stream(query.toLowerCase().split("\\s+"))
                        .collect(Collectors.toSet());

                logger.info("keywords: "+keywords);
                
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                Map<String, String> allFields = hashOps.entries(key);
                
                List<Message> allMessages = new ArrayList<>();
                for (String sessionId : allFields.keySet()) {
                    String messagesJson = allFields.get(sessionId);
                    List<Message> sessionMessages = deserialize(messagesJson);
                    allMessages.addAll(sessionMessages);
                }
                
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
            } catch (Exception e) {
                throw new RuntimeException("Failed to search memory in Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = getUserKey(userId);
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                Map<String, String> allFields = hashOps.entries(key);
                
                List<Message> allMessages = new ArrayList<>();
                for (String sessionId : allFields.keySet().stream().sorted().collect(Collectors.toList())) {
                    String messagesJson = allFields.get(sessionId);
                    List<Message> sessionMessages = deserialize(messagesJson);
                    allMessages.addAll(sessionMessages);
                }
                
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
            } catch (Exception e) {
                throw new RuntimeException("Failed to list memory from Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = getUserKey(userId);
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                
                if (sessionId.isPresent()) {
                    hashOps.delete(key, sessionId.get());
                } else {
                    redisTemplate.delete(key);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete memory from Redis", e);
            }
        });
    }
    
    /**
     * Clear all memory data
     */
    public CompletableFuture<Void> clearAllMemory() {
        return CompletableFuture.runAsync(() -> {
            try {
                Set<String> keys = redisTemplate.keys(getUserKey("*"));
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to clear all memory from Redis", e);
            }
        });
    }
    
    /**
     * Delete all memory data for specified user
     */
    public CompletableFuture<Void> deleteUserMemory(String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = getUserKey(userId);
                redisTemplate.delete(key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete user memory from Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> keys = redisTemplate.keys(getUserKey("*"));
                if (keys == null) {
                    return Collections.emptyList();
                }
                
                return keys.stream()
                    .map(key -> key.substring("user_memory:".length()))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException("Failed to get all users from Redis", e);
            }
        });
    }
    
    /**
     * Get query text from message
     */
    private String getQueryText(Message message) {
        if (message == null || message.getContent() == null) {
            return null;
        }

        // Todo: TEST ME
        return message.getContent().stream()
                .filter(content -> "text".equals(content.getType()))
                .map(MessageContent::getText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }
    
}
