package io.agentscope.runtime.engine.schemas.context;

import io.agentscope.runtime.engine.schemas.agent.Message;

import java.util.ArrayList;
import java.util.List;

public class Session {
    
    private String id;
    private String userId;
    private List<Message> messages;
    
    public Session() {
        this.messages = new ArrayList<>();
    }
    
    public Session(String id, String userId) {
        this();
        this.id = id;
        this.userId = userId;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * Add message
     */
    public void addMessage(Message message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }

    /**
     * Get last message
     */
    public Message getLastMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * Get message count
     */
    public int getMessageCount() {
        return messages == null ? 0 : messages.size();
    }
}
