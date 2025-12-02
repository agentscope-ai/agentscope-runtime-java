package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for events in the agent runtime.
 */
public class Event {
    @JsonProperty("sequence_number")
    private Integer sequenceNumber;
    
    @JsonProperty("object")
    private String object;
    
    @JsonProperty("status")
    protected String status;
    
    @JsonProperty("error")
    private Error error;
    
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Error getError() {
        return error;
    }
    
    public void setError(Error error) {
        this.error = error;
    }
    
    public Event created() {
        this.status = RunStatus.CREATED;
        return this;
    }
    
    public Event inProgress() {
        this.status = RunStatus.IN_PROGRESS;
        return this;
    }
    
    public Event completed() {
        this.status = RunStatus.COMPLETED;
        return this;
    }
    
    public Event failed(Error error) {
        this.status = RunStatus.FAILED;
        this.error = error;
        return this;
    }
    
    public Event rejected() {
        this.status = RunStatus.REJECTED;
        return this;
    }
    
    public Event canceled() {
        this.status = RunStatus.CANCELED;
        return this;
    }
}

