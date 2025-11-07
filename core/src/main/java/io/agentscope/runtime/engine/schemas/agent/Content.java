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

package io.agentscope.runtime.engine.schemas.agent;

import java.util.Map;

/**
 * Content base class
 * Corresponds to the Content class in agent_schemas.py of the Python version
 */
public abstract class Content extends Event {
    
    private String type;
    private String object = "content";
    private Integer index;
    private Boolean delta = false;
    private String msgId;
    
    public Content() {
        super();
    }
    
    public Content(String type) {
        super();
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String getObject() {
        return object;
    }
    
    @Override
    public void setObject(String object) {
        this.object = object;
    }
    
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    public Boolean getDelta() {
        return delta;
    }
    
    public void setDelta(Boolean delta) {
        this.delta = delta;
    }
    
    public String getMsgId() {
        return msgId;
    }
    
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
    
    /**
     * Create content from chat completion chunk
     * Corresponds to the from_chat_completion_chunk method of the Python version
     */
    public static Content fromChatCompletionChunk(Map<String, Object> chunk, Integer index) {
        // Implementation needs to be based on the specific chat completion chunk format
        // Temporarily return null, needs to be implemented according to actual requirements
        return null;
    }
}
