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

package com.example.agentscope.service;

import com.example.agentscope.model.ChatRequest;
import com.example.agentscope.model.ChatResponse;
import com.example.agentscope.model.ToolInfo;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.tool.Toolkit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chat service
 * Handle interaction logic with the Agent
 */
@Service
public class ChatService {
    @Autowired
    private ObjectProvider<ReActAgent> agentProvider;

    @Autowired
    private Toolkit toolkit;


    /**
     * Handle chat requests
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("📨 Received user message: " + request.getMessage());
        System.out.println("=".repeat(50));

        try {
            // Create user message
            Msg userMsg = Msg.builder()
                    .name(request.getUserName() != null ? request.getUserName() : "user")
                    .role(MsgRole.USER)
                    .content(List.of(
                            TextBlock.builder()
                                    .text(request.getMessage())
                                    .build()
                    ))
                    .build();

            // Invoke the Agent
            ReActAgent agent = agentProvider.getObject();
            Msg responseMsg = agent.call(userMsg).block();

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("\n✅ Processing completed, duration: " + duration + "ms");
            System.out.println("=".repeat(50) + "\n");

            // Build response
            ChatResponse response = new ChatResponse();
            response.setSuccess(true);
            if (responseMsg != null) {
                response.setMessage(responseMsg.getTextContent());
            }
            response.setAgentName(agent.getName());
            response.setTimestamp(System.currentTimeMillis());
            response.setProcessingTime(duration);

            return response;

        } catch (Exception e) {
            System.err.println("❌ Processing failed: " + e.getMessage());

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Sorry, an issue occurred while processing your request.");
            errorResponse.setError(e.getMessage());
            errorResponse.setTimestamp(System.currentTimeMillis());

            return errorResponse;
        }
    }

    /**
     * Get available tool list
     */
    public List<ToolInfo> getAvailableTools() {
        return toolkit.getToolSchemas().stream()
                .map(schema -> {
                    Map<String, Object> schemaMap = new HashMap<>();
                    ToolInfo info = new ToolInfo();

                    if (schema != null) {
                        info.setName(schema.getParameters().toString());
                        info.setDescription(schema.getDescription());
                        info.setParameters(schema.getParameters());
                    }

                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * Reset conversation memory
     */
    public void resetMemory() {
        // Each request creates a new Agent; memory is not shared by default
        System.out.println("🔄 Reset request received (each request creates a new Agent by default)");
    }
}

