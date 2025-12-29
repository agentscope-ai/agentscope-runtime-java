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
package io.agentscope.runtime.engine.services.memory.persistence.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.alicloud.openservices.tablestore.SyncClient;
import com.aliyun.openservices.tablestore.agent.memory.MemoryStoreImpl;
import com.aliyun.openservices.tablestore.agent.model.MetaType;
import com.aliyun.openservices.tablestore.agent.model.Metadata;
import com.aliyun.openservices.tablestore.agent.util.Pair;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.schemas.Content;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.schemas.Session;
import io.agentscope.runtime.engine.schemas.TextContent;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TableStore-based implementation of session history service
 * Uses Aliyun TableStore MemoryStore for persistent session and message storage
 */
public class TableStoreSessionHistoryService implements SessionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TableStoreSessionHistoryService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SESSION_SECONDARY_INDEX_NAME = "agentscope_runtime_session_secondary_index";
    private static final String MESSAGE_SECONDARY_INDEX_NAME = "agentscope_runtime_message_secondary_index";

    private final SyncClient client;
    private final String sessionTableName;
    private final String messageTableName;
    private final String sessionSecondaryIndexName;
    private final String messageSecondaryIndexName;
    private final List<Pair<String, MetaType>> sessionSecondaryIndexMeta;

    private MemoryStoreImpl memoryStore;

    /**
     * Constructor with default table names
     *
     * @param client TableStore sync client
     */
    public TableStoreSessionHistoryService(SyncClient client) {
        this(client, "agentscope_runtime_session", "agentscope_runtime_message",
             SESSION_SECONDARY_INDEX_NAME, MESSAGE_SECONDARY_INDEX_NAME, Collections.emptyList());
    }

    /**
     * Constructor with custom table names and indexes
     *
     * @param client TableStore sync client
     * @param sessionTableName session table name
     * @param messageTableName message table name
     * @param sessionSecondaryIndexName session secondary index name
     * @param messageSecondaryIndexName message secondary index name
     * @param sessionSecondaryIndexMeta session secondary index metadata
     */
    public TableStoreSessionHistoryService(SyncClient client, String sessionTableName, String messageTableName,
                                          String sessionSecondaryIndexName, String messageSecondaryIndexName,
                                          List<Pair<String, MetaType>> sessionSecondaryIndexMeta) {
        this.client = client;
        this.sessionTableName = sessionTableName;
        this.messageTableName = messageTableName;
        this.sessionSecondaryIndexName = sessionSecondaryIndexName;
        this.messageSecondaryIndexName = messageSecondaryIndexName;
        this.sessionSecondaryIndexMeta = sessionSecondaryIndexMeta != null ?
                sessionSecondaryIndexMeta : Collections.emptyList();
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (memoryStore != null) {
                return;
            }

            memoryStore = MemoryStoreImpl.builder()
                    .client(client)
                    .sessionTableName(sessionTableName)
                    .sessionSecondaryIndexName(sessionSecondaryIndexName)
                    .sessionSecondaryIndexMeta(sessionSecondaryIndexMeta)
                    .messageTableName(messageTableName)
                    .messageSecondaryIndexName(messageSecondaryIndexName)
                    .build();

            memoryStore.initTable();
            logger.info("TableStore session history service started with tables: {}, {}", sessionTableName, messageTableName);
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (memoryStore != null) {
                memoryStore = null;
                logger.info("TableStore session history service stopped");
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.supplyAsync(() -> {
            if (memoryStore == null) {
                logger.warn("TableStore session history service is not started");
                return false;
            }

            try {
                // Try to list sessions to check connection
                memoryStore.listAllSessions();
                return true;
            } catch (Exception e) {
                logger.error("TableStore session history service health check failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Session> createSession(String userId, Optional<String> sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            String sid = sessionId.filter(s -> !s.trim().isEmpty())
                    .orElse(UUID.randomUUID().toString());

            com.aliyun.openservices.tablestore.agent.model.Session tablestoreSession =
                    new com.aliyun.openservices.tablestore.agent.model.Session(userId, sid);

            memoryStore.putSession(tablestoreSession);

            Session session = new Session(sid, userId, new ArrayList<>());
            logger.info("Created session: {} for user: {}", sid, userId);

            return session;
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> getSession(String userId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            com.aliyun.openservices.tablestore.agent.model.Session tablestoreSession =
                    memoryStore.getSession(userId, sessionId);

            if (tablestoreSession == null) {
                // Create a new session if it doesn't exist
                tablestoreSession = new com.aliyun.openservices.tablestore.agent.model.Session(userId, sessionId);
                memoryStore.putSession(tablestoreSession);
            }

            // Load messages for the session
            Iterator<com.aliyun.openservices.tablestore.agent.model.Message> messageIterator =
                    memoryStore.listMessages(sessionId);

            List<Message> messages = new ArrayList<>();
            if (messageIterator != null) {
                while (messageIterator.hasNext()) {
                    com.aliyun.openservices.tablestore.agent.model.Message tablestoreMessage =
                            messageIterator.next();
                    Message message = convertTablestoreMessageToMessage(tablestoreMessage);
                    messages.add(message);
                }
            }

            Session session = new Session(sessionId, userId, messages);
            return Optional.of(session);
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String userId, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            memoryStore.deleteSessionAndMessages(userId, sessionId);
            logger.info("Deleted session: {} for user: {}", sessionId, userId);
        });
    }

    @Override
    public CompletableFuture<List<Session>> listSessions(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            // List all sessions and filter by userId
            Iterator<com.aliyun.openservices.tablestore.agent.model.Session> iterator =
                    memoryStore.listAllSessions();

            if (iterator == null) {
                return Collections.emptyList();
            }

            List<Session> sessions = new ArrayList<>();
            while (iterator.hasNext()) {
                com.aliyun.openservices.tablestore.agent.model.Session tablestoreSession =
                        iterator.next();

                // Filter by userId
                if (userId.equals(tablestoreSession.getUserId())) {
                    // Create session without messages for performance
                    Session session = new Session(
                            tablestoreSession.getSessionId(),
                            tablestoreSession.getUserId(),
                            new ArrayList<>()
                    );
                    sessions.add(session);
                }
            }

            return sessions;
        });
    }

    @Override
    public CompletableFuture<Void> appendMessage(Session session, List<Message> messages) {
        return CompletableFuture.runAsync(() -> {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            // Update the passed session object
            session.getMessages().addAll(messages);

            // Check if session exists in TableStore
            com.aliyun.openservices.tablestore.agent.model.Session tablestoreSession =
                    memoryStore.getSession(session.getUserId(), session.getId());

            if (tablestoreSession != null) {
                // Store messages in TableStore
                for (Message message : messages) {
                    com.aliyun.openservices.tablestore.agent.model.Message tablestoreMessage =
                            convertMessageToTablestoreMessage(message, session);
                    memoryStore.putMessage(tablestoreMessage);
                }

                logger.info("Appended {} messages to session: {}", messages.size(), session.getId());
            } else {
                logger.error("Warning: Session {} not found in TableStore storage for append_message.", session.getId());
            }
        });
    }

    /**
     * Convert Message to TableStore Message
     */
    private com.aliyun.openservices.tablestore.agent.model.Message convertMessageToTablestoreMessage(
            Message message, Session session) {

        String messageId = UUID.randomUUID().toString();
        com.aliyun.openservices.tablestore.agent.model.Message tablestoreMessage =
                new com.aliyun.openservices.tablestore.agent.model.Message(session.getId(), messageId);

        // Extract text content
        String textContent = message.getContent() != null ?
                message.getContent().stream()
                        .filter(content -> "text".equals(content.getType()))
                        .filter(content -> content instanceof TextContent)
                        .map(content -> ((TextContent) content).getText())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("") : "";

        tablestoreMessage.setContent(textContent);

        // Set metadata
        Metadata metadata = new Metadata();
        metadata.put("message_type", message.getType());
        metadata.put("user_id", session.getUserId());

        // Store message content as JSON
        try {
            if (message.getContent() != null) {
                String contentJson = OBJECT_MAPPER.writeValueAsString(message.getContent());
                metadata.put("content_json", contentJson);
            }

            if (message.getMetadata() != null) {
                String messageMetadataJson = OBJECT_MAPPER.writeValueAsString(message.getMetadata());
                metadata.put("message_metadata", messageMetadataJson);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize message: {}", e.getMessage());
        }

        tablestoreMessage.setMetadata(metadata);
        tablestoreMessage.setCreateTime(System.currentTimeMillis());

        return tablestoreMessage;
    }

    /**
     * Convert TableStore Message to Message
     */
    private Message convertTablestoreMessageToMessage(
            com.aliyun.openservices.tablestore.agent.model.Message tablestoreMessage) {

        Message message = new Message();
        Metadata metadata = tablestoreMessage.getMetadata();

        // Restore message type
        String messageTypeStr = metadata.getString("message_type");
        if (messageTypeStr != null) {
            message.setType(messageTypeStr);
        }

        // Restore message content
        try {
            String contentJson = metadata.getString("content_json");
            if (contentJson != null) {
                List<Content> content = OBJECT_MAPPER.readValue(
                        contentJson,
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, TextContent.class)
                );
                message.setContent(content);
            } else {
                // Fallback to plain text content
                String textContent = tablestoreMessage.getContent();
                if (textContent != null && !textContent.isEmpty()) {
                    TextContent mc = new TextContent();
                    mc.setType("text");
                    mc.setText(textContent);
                    message.setContent(Collections.singletonList(mc));
                }
            }

            String messageMetadataJson = metadata.getString("message_metadata");
            if (messageMetadataJson != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMetadata = OBJECT_MAPPER.readValue(
                        messageMetadataJson,
                        Map.class
                );
                message.setMetadata(messageMetadata);
            } else {
                message.setMetadata(new HashMap<>());
            }

            // Add timestamp if available
            if (tablestoreMessage.getCreateTime() != null) {
                if (message.getMetadata() == null) {
                    message.setMetadata(new HashMap<>());
                }
                message.getMetadata().put("createTime", tablestoreMessage.getCreateTime());
            }

        } catch (Exception e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
        }

        return message;
    }
}

