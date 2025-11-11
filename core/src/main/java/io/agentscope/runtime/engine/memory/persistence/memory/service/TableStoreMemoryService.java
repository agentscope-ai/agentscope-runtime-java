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

import com.alicloud.openservices.tablestore.SyncClient;
import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeSearchRequest;
import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeStoreImpl;
import com.aliyun.openservices.tablestore.agent.model.Document;
import com.aliyun.openservices.tablestore.agent.model.DocumentHit;
import com.aliyun.openservices.tablestore.agent.model.Metadata;
import com.aliyun.openservices.tablestore.agent.model.Response;
import com.aliyun.openservices.tablestore.agent.model.filter.Filter;
import com.aliyun.openservices.tablestore.agent.model.filter.Filters;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.memory.service.MemoryService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * TableStore-based implementation of memory service
 * Uses Aliyun TableStore KnowledgeStore for persistent storage with search capabilities
 */
public class TableStoreMemoryService implements MemoryService {

    private static final Logger logger = Logger.getLogger(TableStoreMemoryService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DEFAULT_SESSION_ID = "default";
    private static final String SEARCH_INDEX_NAME = "agentscope_runtime_knowledge_search_index";

    private final SyncClient client;
    private final String tableName;
    private final String searchIndexName;
    private KnowledgeStoreImpl knowledgeStore;

    /**
     * Constructor with default table and index names
     *
     * @param client TableStore sync client
     */
    public TableStoreMemoryService(SyncClient client) {
        this(client, "agentscope_runtime_memory", SEARCH_INDEX_NAME);
    }

    /**
     * Constructor with custom table and index names
     *
     * @param client TableStore sync client
     * @param tableName table name for storing memory documents
     * @param searchIndexName search index name
     */
    public TableStoreMemoryService(SyncClient client, String tableName, String searchIndexName) {
        this.client = client;
        this.tableName = tableName;
        this.searchIndexName = searchIndexName;
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (knowledgeStore != null) {
                return;
            }

            knowledgeStore = KnowledgeStoreImpl.builder()
                    .client(client)
                    .tableName(tableName)
                    .searchIndexName(searchIndexName)
                    .build();

            knowledgeStore.initTable();
            logger.info("TableStore memory service started with table: " + tableName);
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (knowledgeStore != null) {
                knowledgeStore = null;
                logger.info("TableStore memory service stopped");
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.supplyAsync(() -> {
            if (knowledgeStore == null) {
                logger.warning("TableStore memory service is not started");
                return false;
            }

            try {
                // Try to search with empty request to check connection
                KnowledgeSearchRequest request = KnowledgeSearchRequest.builder()
                        .limit(1)
                        .build();
                knowledgeStore.searchDocuments(request);
                return true;
            } catch (Exception e) {
                logger.severe("TableStore memory service health check failed: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            String sid = sessionId.orElse(DEFAULT_SESSION_ID);

            for (Message message : messages) {
                Document document = convertMessageToDocument(message, userId, sid);
                knowledgeStore.putDocument(document);
            }

            logger.fine("Added " + messages.size() + " messages to TableStore for user: " + userId);
        });
    }

    @Override
    public CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages,
                                                         Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }

            Message lastMessage = messages.get(messages.size() - 1);
            String query = getQueryText(lastMessage);

            if (query == null || query.trim().isEmpty()) {
                return Collections.emptyList();
            }

            int topK = 100;
            if (filters.isPresent() && filters.get().containsKey("top_k")) {
                Object topKObj = filters.get().get("top_k");
                if (topKObj instanceof Integer) {
                    topK = (Integer) topKObj;
                }
            }

            // Create filter for user_id
            Filter userFilter = Filters.eq("user_id", userId);

            // Perform full-text search with correct parameters
            Response<DocumentHit> response = knowledgeStore.fullTextSearch(
                    query,
                    null,  // tenantIds (not using multi-tenant)
                    topK,
                    userFilter,
                    null,  // String parameter
                    null   // metadataToGet (get all metadata)
            );

            return response.getHits().stream()
                    .map(hit -> convertDocumentToMessage(hit.getDocument()))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            int pageNum = 1;
            int pageSize = 10;

            if (filters.isPresent()) {
                Map<String, Object> filterMap = filters.get();
                pageNum = (Integer) filterMap.getOrDefault("page_num", 1);
                pageSize = (Integer) filterMap.getOrDefault("page_size", 10);
            }

            if (pageNum < 1 || pageSize < 1) {
                throw new IllegalArgumentException("page_num and page_size must be greater than 0");
            }

            Filter userFilter = Filters.eq("user_id", userId);

            // Search all documents for the user
            KnowledgeSearchRequest request = KnowledgeSearchRequest.builder()
                    .metadataFilter(userFilter)
                    .build();
            Response<DocumentHit> response = knowledgeStore.searchDocuments(request);

            // Manual pagination from results
            List<DocumentHit> allHits = response.getHits();
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, allHits.size());

            if (startIndex >= allHits.size()) {
                return Collections.emptyList();
            }

            return allHits.subList(startIndex, endIndex).stream()
                    .map(hit -> convertDocumentToMessage(hit.getDocument()))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            Filter filter;

            if (sessionId.isPresent()) {
                // Delete by user_id and session_id
                filter = Filters.and(
                        Filters.eq("user_id", userId),
                        Filters.eq("session_id", sessionId.get())
                );
            } else {
                // Delete all for user_id
                filter = Filters.eq("user_id", userId);
            }

            // Search all documents matching the filter
            KnowledgeSearchRequest request = KnowledgeSearchRequest.builder()
                    .metadataFilter(filter)
                    .build();
            Response<DocumentHit> response = knowledgeStore.searchDocuments(request);

            // Delete each document
            for (DocumentHit hit : response.getHits()) {
                knowledgeStore.deleteDocument(hit.getDocument().getDocumentId());
            }

            logger.fine("Deleted memory for user: " + userId +
                       (sessionId.isPresent() ? ", session: " + sessionId.get() : ""));
        });
    }

    @Override
    public CompletableFuture<List<String>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            // Search all documents
            KnowledgeSearchRequest request = KnowledgeSearchRequest.builder().build();
            Response<DocumentHit> response = knowledgeStore.searchDocuments(request);

            // Extract unique user IDs
            Set<String> userIds = new HashSet<>();
            for (DocumentHit hit : response.getHits()) {
                Metadata metadata = hit.getDocument().getMetadata();
                String userId = metadata.getString("user_id");
                if (userId != null) {
                    userIds.add(userId);
                }
            }

            return new ArrayList<>(userIds);
        });
    }

    /**
     * Convert Message to TableStore Document
     */
    private Document convertMessageToDocument(Message message, String userId, String sessionId) {
        String documentId = UUID.randomUUID().toString();
        Document document = new Document(documentId);

        // Extract text content for indexing
        String textContent = getQueryText(message);
        document.setText(textContent);

        // Add metadata
        Metadata metadata = new Metadata();
        metadata.put("user_id", userId);
        metadata.put("session_id", sessionId);
        metadata.put("message_type", message.getType().name());

        // Store message content as JSON
        try {
            String contentJson = OBJECT_MAPPER.writeValueAsString(message.getContent());
            metadata.put("content", contentJson);

            if (message.getMetadata() != null) {
                String metadataJson = OBJECT_MAPPER.writeValueAsString(message.getMetadata());
                metadata.put("message_metadata", metadataJson);
            }
        } catch (Exception e) {
            logger.severe("Failed to serialize message content: " + e.getMessage());
        }

        document.setMetadata(metadata);
        return document;
    }

    /**
     * Convert TableStore Document to Message
     */
    private Message convertDocumentToMessage(Document document) {
        Metadata metadata = document.getMetadata();

        Message message = new Message();

        // Restore message type
        String messageTypeStr = metadata.getString("message_type");
        if (messageTypeStr != null) {
            message.setType(MessageType.valueOf(messageTypeStr));
        }

        // Restore message content
        try {
            String contentJson = metadata.getString("content");
            if (contentJson != null) {
                List<MessageContent> content = OBJECT_MAPPER.readValue(
                        contentJson,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, MessageContent.class)
                );
                message.setContent(content);
            }

            String messageMetadataJson = metadata.getString("message_metadata");
            if (messageMetadataJson != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMetadata = OBJECT_MAPPER.readValue(
                        messageMetadataJson,
                        Map.class
                );
                message.setMetadata(messageMetadata);
            }
        } catch (Exception e) {
            logger.severe("Failed to deserialize message content: " + e.getMessage());
        }

        return message;
    }

    /**
     * Extract query text from message
     */
    private String getQueryText(Message message) {
        if (message == null || message.getContent() == null) {
            return "";
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

