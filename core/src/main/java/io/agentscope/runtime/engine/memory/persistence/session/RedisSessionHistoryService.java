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

import io.agentscope.runtime.engine.schemas.message.Message;
import io.agentscope.runtime.engine.schemas.context.Session;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Redis-based session history service implementation
 */
public class RedisSessionHistoryService implements SessionHistoryService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisSessionHistoryService(RedisTemplate<String, String> redisTemplate) {
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
    
    private String getSessionKey(String userId, String sessionId) {
        return "session:" + userId + ":" + sessionId;
    }
    
    private String getIndexKey(String userId) {
        return "session_index:" + userId;
    }
    
    private String sessionToJson(Session session) throws JsonProcessingException {
        return objectMapper.writeValueAsString(session);
    }
    
    private Session sessionFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, Session.class);
    }
    
    @Override
    public CompletableFuture<Session> createSession(String userId, Optional<String> sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sid = sessionId.filter(s -> s != null && !s.trim().isEmpty())
                        .orElse(UUID.randomUUID().toString());
                
                Session session = new Session(sid, userId, new ArrayList<>());
                String key = getSessionKey(userId, sid);
                
                redisTemplate.opsForValue().set(key, sessionToJson(session));
                redisTemplate.opsForSet().add(getIndexKey(userId), sid);
                
                return session;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create session in Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Optional<Session>> getSession(String userId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = getSessionKey(userId, sessionId);
                String sessionJson = redisTemplate.opsForValue().get(key);
                
                if (sessionJson == null) {
                    Session session = new Session(sessionId, userId, new ArrayList<>());
                    redisTemplate.opsForValue().set(key, sessionToJson(session));
                    redisTemplate.opsForSet().add(getIndexKey(userId), sessionId);
                    return Optional.of(session);
                }
                
                return Optional.of(sessionFromJson(sessionJson));
            } catch (Exception e) {
                throw new RuntimeException("Failed to get session from Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deleteSession(String userId, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = getSessionKey(userId, sessionId);
                redisTemplate.delete(key);
                redisTemplate.opsForSet().remove(getIndexKey(userId), sessionId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete session from Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Session>> listSessions(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String indexKey = getIndexKey(userId);
                Set<String> sessionIds = redisTemplate.opsForSet().members(indexKey);
                
                if (sessionIds == null) {
                    return Collections.emptyList();
                }
                
                List<Session> sessions = new ArrayList<>();
                for (String sessionId : sessionIds) {
                    String key = getSessionKey(userId, sessionId);
                    String sessionJson = redisTemplate.opsForValue().get(key);
                    if (sessionJson != null) {
                        Session session = sessionFromJson(sessionJson);
                        session.setMessages(new ArrayList<>());
                        sessions.add(session);
                    }
                }
                
                return sessions;
            } catch (Exception e) {
                throw new RuntimeException("Failed to list sessions from Redis", e);
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
                
                // Update the passed session object
                session.getMessages().addAll(messages);
                
                String userId = session.getUserId();
                String sessionId = session.getId();
                String key = getSessionKey(userId, sessionId);
                
                String sessionJson = redisTemplate.opsForValue().get(key);
                if (sessionJson != null) {
                    Session storedSession = sessionFromJson(sessionJson);
                    storedSession.getMessages().addAll(messages);
                    redisTemplate.opsForValue().set(key, sessionToJson(storedSession));
                    redisTemplate.opsForSet().add(getIndexKey(userId), sessionId);
                } else {
                    System.err.println("Warning: Session " + session.getId() + 
                            " not found in storage for append_message.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to append message to session in Redis", e);
            }
        });
    }
    
    /**
     * Delete all session history data for specified user
     */
    public CompletableFuture<Void> deleteUserSessions(String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String indexKey = getIndexKey(userId);
                Set<String> sessionIds = redisTemplate.opsForSet().members(indexKey);
                
                if (sessionIds != null) {
                    for (String sessionId : sessionIds) {
                        String key = getSessionKey(userId, sessionId);
                        redisTemplate.delete(key);
                    }
                }
                
                redisTemplate.delete(indexKey);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete user sessions from Redis", e);
            }
        });
    }
}
