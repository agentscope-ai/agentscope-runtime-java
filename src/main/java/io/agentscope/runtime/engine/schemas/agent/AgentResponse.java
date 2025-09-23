package io.agentscope.runtime.engine.schemas.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent response class
 * Corresponds to the AgentResponse class in agent_schemas.py of the Python version
 */
public class AgentResponse extends Event {
    
    private String id;
    private String object = "response";
    private Long createdAt;
    private Long completedAt;
    private List<Message> output;
    private Map<String, Object> usage;
    private String sessionId;
    
    public AgentResponse() {
        super();
        this.id = "response_" + UUID.randomUUID().toString();
        this.status = RunStatus.CREATED;
        this.createdAt = System.currentTimeMillis();
        this.output = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String getObject() {
        return object;
    }
    
    @Override
    public void setObject(String object) {
        this.object = object;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }
    
    public List<Message> getOutput() {
        return output;
    }
    
    public void setOutput(List<Message> output) {
        this.output = output != null ? output : new ArrayList<>();
    }
    
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Add new message
     * Corresponds to the add_new_message method in the Python version
     */
    public void addNewMessage(Message message) {
        if (output == null) {
            output = new ArrayList<>();
        }
        output.add(message);
    }
}
