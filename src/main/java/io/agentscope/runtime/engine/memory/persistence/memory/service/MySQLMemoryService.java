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
import io.agentscope.runtime.engine.memory.service.EmbeddingService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.service.MemoryProperties;
import io.agentscope.runtime.engine.memory.persistence.memory.entity.MemoryEntity;
import io.agentscope.runtime.engine.memory.persistence.memory.repository.MemoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * MySQL-based memory service implementation
 */
public class MySQLMemoryService implements MemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MySQLMemoryService.class);
    private static final String DEFAULT_SESSION_ID = "default_session";
    
    private MemoryRepository memoryRepository;
    private ObjectMapper objectMapper;
    private EmbeddingService embeddingService;
    private MemoryProperties memoryProperties;
    
    public void setMemoryRepository(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }
    
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public void setEmbeddingService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    public void setMemoryProperties(MemoryProperties memoryProperties) {
        this.memoryProperties = memoryProperties;
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
                // Simple health check: try to query the database
                memoryRepository.count();
                return true;
            } catch (Exception e) {
                logger.error("MySQL memory service health check failed", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sessionIdValue = sessionId.orElse(DEFAULT_SESSION_ID);
                
                for (Message message : messages) {
                    MemoryEntity entity = new MemoryEntity();
                    entity.setUserId(userId);
                    entity.setSessionId(sessionIdValue);
                    entity.setMessageType(message.getType());
                    entity.setContent(serializeMessageContent(message.getContent()));
                    entity.setMetadata(serializeMetadata(message.getMetadata()));
                    
                    // Generate embedding
                    String text = extractTextFromMessage(message);
                    if (text != null && !text.trim().isEmpty()) {
                        List<Double> embedding = embeddingService.embedText(text).get();
                        entity.setEmbedding(serializeEmbedding(embedding));
                    }
                    
                    memoryRepository.save(entity);
                }
                
                logger.info("Successfully added {} memories to MySQL, user: {}, session: {}",
                    messages.size(), userId, sessionIdValue);
                    
            } catch (Exception e) {
                logger.error("Failed to add memory to MySQL", e);
                throw new RuntimeException("Failed to add memory to MySQL", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (messages == null || messages.isEmpty()) {
                    return Collections.emptyList();
                }
                
                // Use the content of the last message as search query
                Message lastMessage = messages.get(messages.size() - 1);
                String queryText = extractTextFromMessage(lastMessage);
                if (queryText == null || queryText.trim().isEmpty()) {
                    return Collections.emptyList();
                }
                
                // Generate query embedding
                List<Double> queryEmbedding = embeddingService.embedText(queryText).get();
                
                // Get all user memories
                List<MemoryEntity> allMemories = memoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

                // Calculate similarity and sort
                List<MemorySimilarity> similarities = allMemories.stream()
                    .filter(memory -> memory.getEmbedding() != null && !memory.getEmbedding().trim().isEmpty())
                    .map(memory -> {
                        try {
                            List<Double> memoryEmbedding = deserializeEmbedding(memory.getEmbedding());
                            double similarity = embeddingService.cosineSimilarity(queryEmbedding, memoryEmbedding);
                            return new MemorySimilarity(memory, similarity);
                        } catch (Exception e) {
                            logger.warn("Failed to calculate similarity, memory ID: {}", memory.getId(), e);
                            return new MemorySimilarity(memory, 0.0);
                        }
                    })
//                    .filter(ms -> ms.similarity > 0.1) // Filter low similarity results
                    .sorted((a, b) -> Double.compare(b.similarity, a.similarity)) // Sort by similarity descending
                    .collect(Collectors.toList());
                
                // Deduplicate: keep only the latest memory for the same content
                Map<String, MemorySimilarity> deduplicated = new LinkedHashMap<>();
                for (MemorySimilarity ms : similarities) {
                    String content = ms.memory.getContent();
                    if (!deduplicated.containsKey(content) || 
                        ms.memory.getCreatedAt().isAfter(deduplicated.get(content).memory.getCreatedAt())) {
                        deduplicated.put(content, ms);
                    }
                }
                // Get top_k results
                int topK = memoryProperties != null ? memoryProperties.getDefaultTopK() : 10;
                if (filters.isPresent() && filters.get().containsKey("top_k")) {
                    Object topKObj = filters.get().get("top_k");
                    if (topKObj instanceof Integer) {
                        topK = (Integer) topKObj;
                    }
                }
                
                List<Message> results = deduplicated.values().stream()
                    .limit(topK)
                    .map(ms -> convertToMessage(ms.memory))
                    .collect(Collectors.toList());
                
                logger.info("MySQL embedding-based memory search completed, user: {}, query: {}, candidates: {}, after deduplication: {}, returned: {}",
                    userId, queryText, similarities.size(), deduplicated.size(), results.size());
                
                return results;
                
            } catch (Exception e) {
                logger.error("MySQL memory search failed", e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int pageNum = 1;
                int pageSize = memoryProperties != null ? memoryProperties.getDefaultPageSize() : 10;
                
                if (filters.isPresent()) {
                    Map<String, Object> filterMap = filters.get();
                    pageNum = (Integer) filterMap.getOrDefault("page_num", 1);
                    pageSize = (Integer) filterMap.getOrDefault("page_size", 
                        memoryProperties != null ? memoryProperties.getDefaultPageSize() : 10);
                }
                
                Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
                Page<MemoryEntity> memoryPage = memoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
                
                List<Message> results = memoryPage.getContent().stream()
                    .map(this::convertToMessage)
                    .collect(Collectors.toList());
                
                logger.debug("MySQL list memory completed, user: {}, page: {}, page size: {}, result count: {}",
                    userId, pageNum, pageSize, results.size());
                
                return results;
                
            } catch (Exception e) {
                logger.error("MySQL list memory failed", e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (sessionId.isPresent()) {
                    memoryRepository.deleteByUserIdAndSessionId(userId, sessionId.get());
                    logger.debug("Deleted user session memory, user: {}, session: {}", userId, sessionId.get());
                } else {
                    memoryRepository.deleteByUserId(userId);
                    logger.debug("Deleted all user memory, user: {}", userId);
                }
            } catch (Exception e) {
                logger.error("MySQL delete memory failed", e);
                throw new RuntimeException("MySQL delete memory failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> users = memoryRepository.findAllUserIds();
                logger.debug("MySQL get all users completed, user count: {}", users.size());
                return users;
            } catch (Exception e) {
                logger.error("MySQL get all users failed", e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Serialize message content
     */
    private String serializeMessageContent(List<MessageContent> content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize message content", e);
            return "[]";
        }
    }
    
    /**
     * Serialize metadata
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize metadata", e);
            return null;
        }
    }
    
    /**
     * Deserialize message content
     */
    private List<MessageContent> deserializeMessageContent(String contentJson) {
        try {
            if (contentJson == null || contentJson.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(contentJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, MessageContent.class));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize message content", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Deserialize metadata
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeMetadata(String metadataJson) {
        try {
            if (metadataJson == null || metadataJson.trim().isEmpty()) {
                return null;
            }
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize metadata", e);
            return null;
        }
    }
    
    /**
     * Convert MemoryEntity to Message
     */
    private Message convertToMessage(MemoryEntity entity) {
        Message message = new Message();
        message.setType(entity.getMessageType());
        message.setContent(deserializeMessageContent(entity.getContent()));
        message.setMetadata(deserializeMetadata(entity.getMetadata()));
        return message;
    }
    
    /**
     * Extract text content from message
     */
    private String extractTextFromMessage(Message message) {
        if (message == null || message.getContent() == null) {
            return null;
        }
        
        return message.getContent().stream()
            .filter(content -> "text".equals(content.getType()))
            .map(MessageContent::getText)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Serialize embedding vector
     */
    private String serializeEmbedding(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize embedding", e);
            return null;
        }
    }
    
    /**
     * Deserialize embedding vector
     */
    @SuppressWarnings("unchecked")
    private List<Double> deserializeEmbedding(String embeddingJson) {
        try {
            if (embeddingJson == null || embeddingJson.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(embeddingJson, List.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize embedding", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Memory similarity inner class
     */
    private static class MemorySimilarity {
        final MemoryEntity memory;
        final double similarity;
        
        MemorySimilarity(MemoryEntity memory, double similarity) {
            this.memory = memory;
            this.similarity = similarity;
        }
    }
}
