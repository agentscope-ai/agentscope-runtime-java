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
package io.agentscope.runtime.engine.memory.service;

import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.schemas.context.Session;
import io.agentscope.runtime.engine.shared.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Session history management service interface
 * Defines standard interfaces for creating, retrieving, updating, and deleting dialogue sessions
 */
public interface SessionHistoryService extends Service {
    
    /**
     * Create a new session for the specified user
     *
     * @param userId User identifier
     * @param sessionId Optional session ID, automatically generated if null
     * @return CompletableFuture<Session> Asynchronously created new session object
     */
    CompletableFuture<Session> createSession(String userId, Optional<String> sessionId);
    
    /**
     * Retrieve a specific session
     *
     * @param userId User identifier
     * @param sessionId Session identifier to retrieve
     * @return CompletableFuture<Optional<Session>> Asynchronous retrieval result, returns session object if found, otherwise returns empty
     */
    CompletableFuture<Optional<Session>> getSession(String userId, String sessionId);
    
    /**
     * Delete a specific session
     *
     * @param userId User identifier
     * @param sessionId Session identifier to delete
     * @return CompletableFuture<Void> Asynchronous deletion result
     */
    CompletableFuture<Void> deleteSession(String userId, String sessionId);
    
    /**
     * List all sessions for the specified user
     *
     * @param userId User identifier
     * @return CompletableFuture<List<Session>> Asynchronous session list result
     */
    CompletableFuture<List<Session>> listSessions(String userId);
    
    /**
     * Append messages to the history of a specific session
     *
     * @param session Session to append messages to
     * @param messages Message or list of messages to append
     * @return CompletableFuture<Void> Asynchronous append result
     */
    CompletableFuture<Void> appendMessage(Session session, List<Message> messages);
}
