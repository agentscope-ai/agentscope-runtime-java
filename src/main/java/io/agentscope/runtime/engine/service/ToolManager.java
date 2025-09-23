package io.agentscope.runtime.engine.service;

import io.agentscope.runtime.engine.schemas.agent.FunctionCall;
import io.agentscope.runtime.engine.schemas.agent.FunctionCallOutput;
import io.agentscope.runtime.engine.service.ToolAdapter;
import io.agentscope.runtime.engine.service.ToolInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Tool manager interface
 * Corresponds to the tool management functionality in the Python version
 */
public interface ToolManager {
    
    /**
     * Register tool
     */
    void registerTool(String name, ToolAdapter adapter);
    
    /**
     * Unregister tool
     */
    void unregisterTool(String name);
    
    /**
     * Execute tool call
     */
    CompletableFuture<FunctionCallOutput> executeTool(FunctionCall functionCall);
    
    /**
     * Get available tools list
     */
    List<ToolInfo> getAvailableTools();
    
    /**
     * Get tool information
     */
    ToolInfo getToolInfo(String name);
    
    /**
     * Check if tool exists
     */
    boolean toolExists(String name);
    
    /**
     * Get tool schema
     */
    Map<String, Object> getToolSchema(String name);
    
    /**
     * Get all tool schemas
     */
    Map<String, Map<String, Object>> getAllToolSchemas();
}
