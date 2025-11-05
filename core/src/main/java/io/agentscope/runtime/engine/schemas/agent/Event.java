package io.agentscope.runtime.engine.schemas.agent;


/**
 * Event base class
 * Corresponds to the Event class in agent_schemas.py of the Python version
 */
public class Event {
    
    private Integer sequenceNumber;
    protected String object;
    protected String status;
    private java.lang.Error error;
    
    public Event() {
        this.status = RunStatus.CREATED;
    }
    
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
    
    public java.lang.Error getError() {
        return error;
    }
    
    public void setError(java.lang.Error error) {
        this.error = error;
    }
    
    /**
     * Set to created status
     */
    public Event created() {
        this.status = RunStatus.CREATED;
        return this;
    }
    
    /**
     * Set to in progress status
     */
    public Event inProgress() {
        this.status = RunStatus.IN_PROGRESS;
        return this;
    }
    
    /**
     * Set to completed status
     */
    public Event completed() {
        this.status = RunStatus.COMPLETED;
        return this;
    }
    
    /**
     * Set to rejected status
     */
    public Event rejected() {
        this.status = RunStatus.REJECTED;
        return this;
    }
    
    /**
     * Set to canceled status
     */
    public Event canceled() {
        this.status = RunStatus.CANCELED;
        return this;
    }
}
