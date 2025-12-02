package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Agent response class that extends BaseResponse.
 * Includes session_id for conversation tracking.
 */
public class AgentResponse extends BaseResponse {
    @JsonProperty("session_id")
    private String sessionId;
    
    public AgentResponse() {
        super();
    }
    
    public AgentResponse(String id) {
        super(id);
    }
    
    public AgentResponse(String id, String sessionId) {
        super(id);
        this.sessionId = sessionId;
    }
    
    public AgentResponse(String id, String sessionId, Long createdAt) {
        super(id);
        this.sessionId = sessionId;
        this.setCreatedAt(createdAt);
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Set status to created and return self.
     */
    @Override
    public AgentResponse created() {
        super.created();
        return this;
    }
    
    /**
     * Set status to in_progress and return self.
     */
    @Override
    public AgentResponse inProgress() {
        super.inProgress();
        return this;
    }
    
    /**
     * Set status to completed and return self.
     */
    @Override
    public AgentResponse completed() {
        super.completed();
        return this;
    }
    
    /**
     * Set status to failed with error and return self.
     */
    @Override
    public AgentResponse failed(Error error) {
        super.failed(error);
        return this;
    }
}

