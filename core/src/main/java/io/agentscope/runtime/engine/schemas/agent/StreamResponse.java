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

import io.agentscope.runtime.engine.schemas.message.MessageType;

import java.util.HashMap;
import java.util.Map;

public class StreamResponse {
    public String name;
    public String content;
    public MessageType type;
    public String id;

    public StreamResponse(String name, String content, MessageType type, String id){
        this.name = name;
        this.content = content;
        this.id = id;
        this.type = type;
    }

    public StreamResponse(String name, String content, MessageType type) {
        this(name, content, type, null);
    }

    public StreamResponse(String content) {
        this("", content, MessageType.ASSISTANT);
    }

    public StreamResponse() {
        this("");
    }

    public String toString() {
        if (type == MessageType.TOOL_CALL) {
            Map<String, String> map = new HashMap<>();
            map.put("toolInput", content);
            map.put("toolName", name);
            map.put("toolId", id);
            System.out.println(content);
            return map.toString();
        }
        else if(type  == MessageType.TOOL_RESPONSE){
            Map<String, String> map = new HashMap<>();
            map.put("toolName", name);
            map.put("toolId", id);
            map.put("toolResult", content);
            System.out.println(content);
            return map.toString();
        }
        return content;
    }
}