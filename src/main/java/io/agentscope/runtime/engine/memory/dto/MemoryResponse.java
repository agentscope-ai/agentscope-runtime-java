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
package io.agentscope.runtime.engine.memory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MemoryResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("memories")
    private List<MemoryItem> memories;
    
    public MemoryResponse() {}
    
    public MemoryResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public MemoryResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public static MemoryResponse success(String message) {
        return new MemoryResponse(true, message);
    }
    
    public static MemoryResponse success(String message, Object data) {
        return new MemoryResponse(true, message, data);
    }
    
    public static MemoryResponse error(String message) {
        return new MemoryResponse(false, message);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public List<MemoryItem> getMemories() {
        return memories;
    }
    
    public void setMemories(List<MemoryItem> memories) {
        this.memories = memories;
    }
}
