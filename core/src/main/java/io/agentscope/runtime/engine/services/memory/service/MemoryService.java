package io.agentscope.runtime.engine.services.memory.service;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.shared.Service;

/**
 * Memory service interface
 * Used for storing and retrieving long-term memories, supports database or in-memory storage
 * Memory is organized by user ID and supports two management strategies:
 * 1. Messages grouped by session ID (session ID under user ID)
 * 2. Messages grouped only by user ID
 */
public interface MemoryService extends Service {
    
    /**
     * Add messages to memory service
     *
     * @param userId user ID
     * @param messages list of messages to add
     * @param sessionId optional session ID
     * @return CompletableFuture<Void> asynchronous addition result
     */
    CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId);
    
    /**
     * Search messages from memory service
     *
     * @param userId user ID
     * @param messages user query or query with history messages, all in message list format
     * @param filters filters for searching memory
     * @return CompletableFuture<List<Message>> asynchronous search result
     */
    CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages, Optional<Map<String, Object>> filters);
    
    /**
     * List memory items for specified user, supports pagination and other filters
     *
     * @param userId user ID
     * @param filters filters for memory items, such as page_num, page_size, etc.
     * @return CompletableFuture<List<Message>> asynchronous list result
     */
    CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters);
    
    /**
     * Delete memory items for specified user
     *
     * @param userId user ID
     * @param sessionId optional session ID, if provided only delete messages for that session, otherwise delete all messages for the user
     * @return CompletableFuture<Void> asynchronous deletion result
     */
    CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId);
    
    /**
     * Get all users list
     *
     * @return CompletableFuture<List<String>> asynchronous user list result
     */
    CompletableFuture<List<String>> getAllUsers();
}
