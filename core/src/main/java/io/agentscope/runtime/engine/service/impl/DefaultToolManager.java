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

package io.agentscope.runtime.engine.service.impl;

import io.agentscope.runtime.engine.service.ToolAdapter;
import io.agentscope.runtime.engine.service.ToolInfo;
import io.agentscope.runtime.engine.service.ToolManager;
import io.agentscope.runtime.engine.schemas.agent.FunctionCall;
import io.agentscope.runtime.engine.schemas.agent.FunctionCallOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default tool manager implementation
 * Corresponds to the tool management functionality in the Python version
 */
public class DefaultToolManager implements ToolManager {
    
    private final Map<String, ToolAdapter> tools;
    private final Map<String, ToolInfo> toolInfos;
    
    public DefaultToolManager() {
        this.tools = new ConcurrentHashMap<>();
        this.toolInfos = new ConcurrentHashMap<>();
    }
    
    @Override
    public void registerTool(String name, ToolAdapter adapter) {
        if (name == null || adapter == null) {
            throw new IllegalArgumentException("BaseSandboxTool name and adapter cannot be null");
        }
        
        tools.put(name, adapter);
        
        // Create tool information
        ToolInfo toolInfo = new ToolInfo(
            name,
            adapter.getDescription(),
            adapter.getSchema(),
            "mcp_server",
            "basic"
        );
        toolInfos.put(name, toolInfo);
    }
    
    @Override
    public void unregisterTool(String name) {
        tools.remove(name);
        toolInfos.remove(name);
    }
    
    @Override
    public CompletableFuture<FunctionCallOutput> executeTool(FunctionCall functionCall) {
        if (functionCall == null || functionCall.getName() == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Function call and name cannot be null")
            );
        }
        
        ToolAdapter adapter = tools.get(functionCall.getName());
        if (adapter == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("BaseSandboxTool not found: " + functionCall.getName())
            );
        }
        
        return adapter.execute(functionCall);
    }
    
    @Override
    public List<ToolInfo> getAvailableTools() {
        return new ArrayList<>(toolInfos.values());
    }
    
    @Override
    public ToolInfo getToolInfo(String name) {
        return toolInfos.get(name);
    }
    
    @Override
    public boolean toolExists(String name) {
        return tools.containsKey(name);
    }
    
    @Override
    public Map<String, Object> getToolSchema(String name) {
        ToolInfo toolInfo = toolInfos.get(name);
        return toolInfo != null ? toolInfo.getSchema() : null;
    }
    
    @Override
    public Map<String, Map<String, Object>> getAllToolSchemas() {
        Map<String, Map<String, Object>> schemas = new HashMap<>();
        for (Map.Entry<String, ToolInfo> entry : toolInfos.entrySet()) {
            schemas.put(entry.getKey(), entry.getValue().getSchema());
        }
        return schemas;
    }
}
