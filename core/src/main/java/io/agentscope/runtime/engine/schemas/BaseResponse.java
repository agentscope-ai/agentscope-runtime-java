package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base response class for agent responses.
 * Extends Event to include response-specific fields.
 */
public class BaseResponse extends Event {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("object")
    private String object = "response";
    
    @JsonProperty("created_at")
    private Long createdAt;
    
    @JsonProperty("completed_at")
    private Long completedAt;
    
    @JsonProperty("output")
    private List<Message> output;
    
    @JsonProperty("usage")
    private Map<String, Object> usage;
    
    public BaseResponse() {
        super();
        this.id = "response_" + UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis() / 1000; // Unix timestamp
    }
    
    public BaseResponse(String id) {
        super();
        this.id = id != null ? id : "response_" + UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis() / 1000;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
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
        this.output = output;
    }
    
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
    
    /**
     * Add a new message to the output list.
     */
    public void addNewMessage(Message message) {
        if (this.output == null) {
            this.output = new java.util.ArrayList<>();
        }
        this.output.add(message);
    }
    
    /**
     * Set status to created and return self.
     */
    public BaseResponse created() {
        super.created();
        return this;
    }
    
    /**
     * Set status to in_progress and return self.
     */
    public BaseResponse inProgress() {
        super.inProgress();
        return this;
    }
    
    /**
     * Set status to completed and return self.
     */
    public BaseResponse completed() {
        super.completed();
        this.completedAt = System.currentTimeMillis() / 1000;
        return this;
    }
    
    /**
     * Set status to failed with error and return self.
     */
    public BaseResponse failed(Error error) {
        super.failed(error);
        return this;
    }
}

