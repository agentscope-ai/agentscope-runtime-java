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

package io.agentscope.runtime.engine.schemas.message;


import io.agentscope.runtime.engine.schemas.agent.RunStatus;

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
