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
package io.agentscope.runtime.engine.memory.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.shared.ServiceManager;

/**
 * Context manager
 * Manages the combination of session history and memory services
 */
public class ContextManager extends ServiceManager {

    private final Class<? extends ContextComposer> contextComposerClass;
    private SessionHistoryService sessionHistoryService;
    private MemoryService memoryService;

    public ContextManager() {
        this.contextComposerClass = ContextComposer.class;
        this.sessionHistoryService = null;
        this.memoryService = null;
    }

    public ContextManager(
            Class<? extends ContextComposer> contextComposerClass,
            SessionHistoryService sessionHistoryService,
            MemoryService memoryService) {
        this.contextComposerClass = contextComposerClass;
        this.sessionHistoryService = sessionHistoryService;
        this.memoryService = memoryService;
    }

    @Override
    protected void registerDefaultServices() {
        // Register default services for context management
        this.sessionHistoryService = this.sessionHistoryService != null ?
                this.sessionHistoryService : new InMemorySessionHistoryService();
        this.memoryService = this.memoryService != null ?
                this.memoryService : new InMemoryMemoryService();

        registerService("session", this.sessionHistoryService);
        registerService("memory", this.memoryService);
    }

    /**
     * Compose context information
     *
     * @param session session object
     * @param requestInput request input messages
     * @return CompletableFuture<Void> asynchronous composition result
     */
    public CompletableFuture<Void> composeContext(Session session, List<Message> requestInput) {
        return ContextComposer.compose(
                requestInput,
                session,
                Optional.ofNullable(this.memoryService),
                Optional.ofNullable(this.sessionHistoryService)
        );
    }

    /**
     * Compose session
     *
     * @param userId user ID
     * @param sessionId session ID
     * @return CompletableFuture<Session> asynchronous session result
     */
    public CompletableFuture<Session> composeSession(String userId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (sessionHistoryService != null) {
                    Optional<Session> sessionOpt = sessionHistoryService.getSession(userId, sessionId).get();
                    if (sessionOpt.isEmpty()) {
                        // If Session doesn't exist, create a new one
                        return sessionHistoryService.createSession(userId, Optional.of(sessionId)).get();
                    }
                    return sessionOpt.get();
                } else {
                    // If retrieval fails, create a new Session
                    return new Session(sessionId, userId, new ArrayList<>());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to compose session", e);
            }
        });
    }

    /**
     * Append messages to session
     *
     * @param session session object
     * @param eventOutput event output messages
     * @return CompletableFuture<Void> asynchronous append result
     */
    public CompletableFuture<Void> append(Session session, List<Message> eventOutput) {
        return CompletableFuture.runAsync(() -> {
            try {
                SessionHistoryService sessionHistoryService = getSessionHistoryService();
                if (sessionHistoryService != null) {
                    sessionHistoryService.appendMessage(session, eventOutput).get();
                }

                MemoryService memoryService = getMemoryService();
                if (memoryService != null) {
                    memoryService.addMemory(session.getUserId(), eventOutput, Optional.of(session.getId())).get();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to append messages", e);
            }
        });
    }

    /**
     * Get session history service
     */
    public SessionHistoryService getSessionHistoryService() {
        return sessionHistoryService;
    }

    /**
     * Get memory service
     */
    public MemoryService getMemoryService() {
        return memoryService;
    }
}
