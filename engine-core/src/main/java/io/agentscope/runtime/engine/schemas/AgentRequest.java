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

package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Agent request model.
 */
public class AgentRequest extends BaseRequest {
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("top_p")
    private Double topP;
    
    @JsonProperty("temperature")
    private Double temperature;
    
    @JsonProperty("input")
    private List<Message> input;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("user_id")
    private String userId;
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public List<Message> getInput() {
        return input;
    }
    
    public void setInput(List<Message> input) {
        this.input = input;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}

