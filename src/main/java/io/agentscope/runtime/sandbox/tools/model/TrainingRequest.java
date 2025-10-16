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
package io.agentscope.runtime.sandbox.tools.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request model for training sandbox operations
 */
public class TrainingRequest {
    @JsonProperty("env_type")
    private String envType;
    
    @JsonProperty("task_id")
    private String taskId;
    
    @JsonProperty("instance_id")
    private String instanceId;
    
    @JsonProperty("messages")
    private Map<String, Object> messages;
    
    @JsonProperty("params")
    private Map<String, Object> params;
    
    @JsonProperty("split")
    private String split;
    
    public TrainingRequest() {}
    
    public String getEnvType() {
        return envType;
    }
    
    public void setEnvType(String envType) {
        this.envType = envType;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public Map<String, Object> getMessages() {
        return messages;
    }
    
    public void setMessages(Map<String, Object> messages) {
        this.messages = messages;
    }
    
    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
    
    public String getSplit() {
        return split;
    }
    
    public void setSplit(String split) {
        this.split = split;
    }
}
