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

import io.agentscope.runtime.engine.schemas.agent.ImageContent;
import io.agentscope.runtime.engine.schemas.agent.RunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Message extends Event {

    private String id;
    private MessageType type = MessageType.ASSISTANT;
    private List<Content> content;
    private TokenUsage tokenUsage;
    private Map<String, Object> metadata;

    public Message() {
        super();
        this.id = "msg_" + UUID.randomUUID().toString();
        this.object = "message";
        this.status = RunStatus.CREATED;
        this.content = new ArrayList<>();
    }

    public Message(MessageType type) {
        this();
        this.type = type;
    }

    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public List<Content> getContent() {
        return content;
    }

    public void setContent(List<Content> content) {
        this.content = content != null ? content : new ArrayList<>();
    }

    public String getTextContent() {
        if (content == null) {
            return null;
        }

        for (Content item : content) {
            if (item instanceof TextContent) {
                return ((TextContent) item).getText();
            }
        }
        return null;
    }

    public List<String> getImageContent() {
        List<String> images = new ArrayList<>();

        if (content == null) {
            return images;
        }

        for (Content item : content) {
            if (item instanceof ImageContent) {
                images.add(((ImageContent) item).getImageUrl());
            }
        }
        return images;
    }

    private Content copyContent(Content original) {
        if (original instanceof TextContent text) {
            return new TextContent(text.getText());
        } else if (original instanceof ImageContent image) {
            return new ImageContent(image.getImageUrl());
        } else if (original instanceof DataContent data) {
            return new DataContent(data.getData());
        }
        return original;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
