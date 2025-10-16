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
package io.agentscope.runtime.engine.memory.persistence.session;

import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.entity.SessionEntity;
import io.agentscope.runtime.engine.memory.persistence.memory.entity.SessionMessageEntity;
import io.agentscope.runtime.engine.memory.persistence.memory.repository.SessionMessageRepository;
import io.agentscope.runtime.engine.memory.persistence.memory.repository.SessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * MySQL-based session history service implementation
 */
public class MySQLSessionHistoryService implements SessionHistoryService {

    Logger logger = Logger.getLogger(MySQLSessionHistoryService.class.getName());

    private SessionRepository sessionRepository;
    private SessionMessageRepository sessionMessageRepository;
    private ObjectMapper objectMapper;

    public void setSessionRepository(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void setSessionMessageRepository(SessionMessageRepository sessionMessageRepository) {
        this.sessionMessageRepository = sessionMessageRepository;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
                sessionRepository.count();
                return true;
            } catch (Exception e) {
                logger.severe("MySQL session service health check failed" + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Session> createSession(String userId, Optional<String> sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sid = sessionId.filter(s -> s != null && !s.trim().isEmpty())
                        .orElse(UUID.randomUUID().toString());

                // Check if the session already exists
                if (sessionRepository.existsBySessionId(sid)) {
                    logger.warning("Session already exists: " + sid);
                    Optional<Session> existingSession = getSession(userId, sid).get();
                    return existingSession.orElseThrow(() -> new RuntimeException("Failed to get existing session"));
                }

                SessionEntity entity = new SessionEntity(sid, userId);
                sessionRepository.save(entity);

                Session session = new Session(sid, userId, new ArrayList<>());
                logger.info("Session created successfully, user: " + userId + ", session: " + sid);

                return session;

            } catch (Exception e) {
                logger.severe("Failed to create session" + e.getMessage());
                throw new RuntimeException("Failed to create session", e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> getSession(String userId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<SessionEntity> entityOpt = sessionRepository.findByUserIdAndSessionId(userId, sessionId);

                if (entityOpt.isPresent()) {
                    List<Message> messages = getSessionMessages(sessionId);

                    Session session = new Session(sessionId, userId, messages);
                    logger.info("Session retrieved successfully, user: " + userId + ", session: " + sessionId + ", message count: " + messages.size());

                    return Optional.of(session);
                } else {
                    // If the session does not exist, create a new one
                    logger.info("Session does not exist, creating a new session, user: " + userId + ", session: " + sessionId);
                    Session newSession = createSession(userId, Optional.of(sessionId)).get();
                    return Optional.of(newSession);
                }

            } catch (Exception e) {
                logger.severe("Failed to get session" + e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String userId, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // First delete session messages
                sessionMessageRepository.deleteBySessionId(sessionId);

                // Then delete the session
                sessionRepository.deleteBySessionId(sessionId);

                logger.info("Session deleted successfully, user: " + userId + ", session: " + sessionId);

            } catch (Exception e) {
                logger.severe("Failed to delete session" + e.getMessage());
                throw new RuntimeException("Failed to delete session", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Session>> listSessions(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SessionEntity> entities = sessionRepository.findByUserIdOrderByLastActivityDesc(userId);

                List<Session> sessions = entities.stream()
                        .map(entity -> {
                            Session session = new Session(entity.getSessionId(), entity.getUserId(), new ArrayList<>());
                            return session;
                        })
                        .collect(Collectors.toList());

                logger.info("Listed user sessions successfully, user: " + userId + ", session count: " + sessions.size());

                return sessions;

            } catch (Exception e) {
                logger.severe("Failed to list user sessions" + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Void> appendMessage(Session session, List<Message> messages) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (messages == null || messages.isEmpty()) {
                    return;
                }

                // Update session activity time
                Optional<SessionEntity> entityOpt = sessionRepository.findBySessionId(session.getId());
                if (entityOpt.isPresent()) {
                    SessionEntity entity = entityOpt.get();
                    entity.setLastActivity(new java.sql.Timestamp(System.currentTimeMillis()).toLocalDateTime());
                    sessionRepository.save(entity);
                }

                // Add messages to the session
                for (Message message : messages) {
                    SessionMessageEntity messageEntity = new SessionMessageEntity();
                    messageEntity.setSessionId(session.getId());
                    messageEntity.setMessageType(message.getType());
                    messageEntity.setContent(serializeMessageContent(message.getContent()));
                    messageEntity.setMetadata(serializeMetadata(message.getMetadata()));

                    sessionMessageRepository.save(messageEntity);
                }

                // Update session object
                session.getMessages().addAll(messages);

                logger.info("Messages appended to session successfully, session: " + session.getId() + ", message count: " + messages.size());

            } catch (Exception e) {
                logger.severe("Failed to append messages to session"+ e.getMessage());
                throw new RuntimeException("Failed to append messages to session", e);
            }
        });
    }

    /**
     * Get all messages of the session
     */
    private List<Message> getSessionMessages(String sessionId) {
        try {
            List<SessionMessageEntity> messageEntities = sessionMessageRepository
                    .findBySessionIdOrderByCreatedAtAsc(sessionId);

            return messageEntities.stream()
                    .map(this::convertToMessage)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.severe("Failed to get session messages"+ e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Serialize message content
     */
    private String serializeMessageContent(List<MessageContent> content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.severe("Failed to serialize message content"+ e.getMessage());
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
            logger.severe("Failed to serialize metadata"+ e.getMessage());
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
            logger.severe("Failed to deserialize message content"+ e.getMessage());
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
            logger.severe("Failed to deserialize metadata"+ e.getMessage());
            return null;
        }
    }

    /**
     * Convert SessionMessageEntity to Message
     */
    private Message convertToMessage(SessionMessageEntity entity) {
        Message message = new Message();
        message.setType(entity.getMessageType());
        message.setContent(deserializeMessageContent(entity.getContent()));
        message.setMetadata(deserializeMetadata(entity.getMetadata()));
        return message;
    }
}
