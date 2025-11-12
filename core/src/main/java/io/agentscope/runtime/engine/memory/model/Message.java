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
package io.agentscope.runtime.engine.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.runtime.engine.schemas.agent.MessageType;

import java.util.List;
import java.util.Map;

/**
 * Message class, represents a message in a conversation
 */
public class Message {
    
    @JsonProperty("type")
    private MessageType type;
    
    @JsonProperty("content")
    private List<MessageContent> content;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    public Message() {}
    
    public Message(MessageType type, List<MessageContent> content) {
        this.type = type;
        this.content = content;
    }
    
    public Message(MessageType type, List<MessageContent> content, Map<String, Object> metadata) {
        this.type = type;
        this.content = content;
        this.metadata = metadata;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public List<MessageContent> getContent() {
        return content;
    }
    
    public void setContent(List<MessageContent> content) {
        this.content = content;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
