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

package io.agentscope.runtime.adapters.agentscope.memory;

import io.agentscope.core.message.Msg;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.state.StateModuleBase;
import io.agentscope.runtime.adapters.agentscope.AgentScopeMessageAdapter;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.schemas.Session;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * AgentScope Memory implementation based on SessionHistoryService.
 * 
 * <p>This class stores messages in an underlying SessionHistoryService instance,
 *
 * <p>This adapter bridges AgentScope Java framework's Memory interface with
 * the runtime's SessionHistoryService, allowing agents to use runtime-backed
 * session storage.
 * 
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Create session history service (assumed to exist in runtime)
 * SessionHistoryService service = new InMemorySessionHistoryService();
 * 
 * // Create memory adapter
 * AgentScopeMemoryAdapter memory = new AgentScopeMemoryAdapter(
 *     service, "user_123", "session_456"
 * );
 * 
 * // Use in ReActAgent
 * ReActAgent agent = ReActAgent.builder()
 *     .name("Assistant")
 *     .model(model)
 *     .memory(memory)
 *     .build();
 * }</pre>
 * 
 * @see Memory
 * @see AgentScopeMessageAdapter
 */
public class MemoryAdapter extends StateModuleBase implements Memory {
    private static final Logger logger = LoggerFactory.getLogger(MemoryAdapter.class);
    
    private final SessionHistoryService service;
    private final String userId;
    private final String sessionId;
    private final AgentScopeMessageAdapter messageAdapter;
    
    // Cached session to avoid repeated lookups
    private Session session;
    
    /**
     * Creates a new AgentScopeMemoryAdapter.
     * 
     * @param service The backend session history service
     * @param userId The user ID linked to this memory
     * @param sessionId The session ID linked to this memory
     */
    public MemoryAdapter(
            SessionHistoryService service,
            String userId,
            String sessionId) {
        super();
        this.service = service;
        this.userId = userId;
        this.sessionId = sessionId;
        this.messageAdapter = new AgentScopeMessageAdapter();
        this.session = null;
    }
    
    @Override
    public String getComponentName() {
        return "memory";
    }
    
    /**
     * Ensures the session exists in the backend.
     * This method checks and creates the session if needed.
     * For sync purposes, we always reload from backend to stay in sync.
     */
    private CompletableFuture<Void> ensureSession() {
        return service.getSession(userId, sessionId)
            .thenCompose(optionalSession -> {
                if (optionalSession.isPresent()) {
                    // Always reload from backend to stay in sync
                    this.session = optionalSession.get();
                    return CompletableFuture.completedFuture(null);
                } else {
                    return service.createSession(userId, Optional.of(sessionId))
                        .thenAccept(created -> this.session = created);
                }
            });
    }
    
    @Override
    public void addMessage(Msg message) {
        if (message == null) {
            return;
        }
        
        // Ensure session exists
        ensureSession().join();
        
        // Convert AgentScope Msg to runtime Message
        List<Message> runtimeMessages = messageAdapter.frameworkMsgToMessage(message);
        
        // Append to session
        if (session != null) {
            service.appendMessage(session, runtimeMessages).join();
        }
    }
    
    @Override
    public List<Msg> getMessages() {
        // Ensure session exists and reload from backend for sync purposes
        // (matching Python version's behavior of always reloading)
        ensureSession().join();
        
        if (session == null) {
            return new ArrayList<>();
        }
        
        // Get messages from session (always fresh from backend)
        List<Message> runtimeMessages = session.getMessages();
        
        if (runtimeMessages == null || runtimeMessages.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Convert runtime Messages to AgentScope Msgs
        Object agentscopeMsgs = messageAdapter.messageToFrameworkMsg(runtimeMessages);
        
        if (agentscopeMsgs instanceof List) {
            @SuppressWarnings("unchecked")
            List<Msg> msgList = (List<Msg>) agentscopeMsgs;
            return msgList;
        } else if (agentscopeMsgs instanceof Msg) {
            return List.of((Msg) agentscopeMsgs);
        } else {
            logger.warn("Unexpected message conversion result type: {}", 
                      agentscopeMsgs != null ? agentscopeMsgs.getClass().getName() : "null");
            return new ArrayList<>();
        }
    }
    
    @Override
    public void deleteMessage(int index) {
        // Ensure session exists
        ensureSession().join();
        
        if (session == null) {
            return;
        }
        
        List<Msg> currentMessages = getMessages();
        
        // Validate index
        if (index < 0 || index >= currentMessages.size()) {
            logger.warn("Index {} out of bounds for message list of size {}", 
                       index, currentMessages.size());
            return;
        }
        
        // Remove message at index
        List<Msg> updatedMessages = new ArrayList<>();
        for (int i = 0; i < currentMessages.size(); i++) {
            if (i != index) {
                updatedMessages.add(currentMessages.get(i));
            }
        }
        
        // Clear and re-add remaining messages
        clear();
        for (Msg msg : updatedMessages) {
            addMessage(msg);
        }
    }
    
    @Override
    public void clear() {
        service.deleteSession(userId, sessionId).join();
        this.session = null;
    }
    
    @Override
    public Map<String, Object> stateDict() {
        // Memory state is managed by the backend service
        // Return empty state dict as state is persisted in service
        return new HashMap<>();
    }
    
    @Override
    public void loadStateDict(Map<String, Object> stateDict, boolean strict) {
        // Memory state is managed by the backend service
        // No-op as state is persisted in service
    }
    
}

