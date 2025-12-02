package io.agentscope.runtime.adapters.agentscope.memory;

import io.agentscope.core.message.Msg;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.state.StateModuleBase;
import io.agentscope.runtime.adapters.agentscope.AgentScopeMessageAdapter;
import io.agentscope.runtime.engine.schemas.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AgentScope Memory implementation based on SessionHistoryService.
 * 
 * <p>This class stores messages in an underlying SessionHistoryService instance,
 * matching the Python version's AgentScopeSessionHistoryMemory implementation.
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
    
    // TODO: Replace with actual SessionHistoryService interface when available
    // For now, using a placeholder interface
    private final SessionHistoryServiceInterface service;
    private final String userId;
    private final String sessionId;
    private final AgentScopeMessageAdapter messageAdapter;
    
    // Cached session to avoid repeated lookups
    private SessionInterface session;
    
    /**
     * Creates a new AgentScopeMemoryAdapter.
     * 
     * @param service The backend session history service
     * @param userId The user ID linked to this memory
     * @param sessionId The session ID linked to this memory
     */
    public MemoryAdapter(
            SessionHistoryServiceInterface service,
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
     */
    private CompletableFuture<Void> ensureSession() {
        return service.getSession(userId, sessionId)
            .thenCompose(s -> {
                if (s == null) {
                    return service.createSession(userId, sessionId)
                        .thenAccept(created -> this.session = created);
                } else {
                    this.session = s;
                    return CompletableFuture.completedFuture(null);
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
        // Ensure session exists
        ensureSession().join();
        
        if (session == null) {
            return new ArrayList<>();
        }
        
        // Get messages from session
        List<Message> runtimeMessages = session.getMessages();
        
        // Convert runtime Messages to AgentScope Msgs
        Object agentscopeMsgs = messageAdapter.messageToFrameworkMsg(runtimeMessages);
        
        if (agentscopeMsgs instanceof List) {
            @SuppressWarnings("unchecked")
            List<Msg> msgList = (List<Msg>) agentscopeMsgs;
            return msgList;
        } else if (agentscopeMsgs instanceof Msg) {
            return List.of((Msg) agentscopeMsgs);
        } else {
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
    
    // ========== Placeholder Interfaces ==========
    // TODO: Replace with actual runtime service interfaces when available
    
    /**
     * Placeholder interface for SessionHistoryService.
     * This should be replaced with the actual service interface from runtime.
     */
    public interface SessionHistoryServiceInterface {
        CompletableFuture<SessionInterface> getSession(String userId, String sessionId);
        CompletableFuture<SessionInterface> createSession(String userId, String sessionId);
        CompletableFuture<Void> deleteSession(String userId, String sessionId);
        CompletableFuture<Void> appendMessage(SessionInterface session, List<Message> messages);
    }
    
    /**
     * Placeholder interface for Session.
     * This should be replaced with the actual Session class from runtime.
     */
    public interface SessionInterface {
        String getId();
        String getUserId();
        List<Message> getMessages();
    }
}

