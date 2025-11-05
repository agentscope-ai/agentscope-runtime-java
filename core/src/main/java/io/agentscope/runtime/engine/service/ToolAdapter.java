package io.agentscope.runtime.engine.service;

import io.agentscope.runtime.engine.schemas.agent.FunctionCall;
import io.agentscope.runtime.engine.schemas.agent.FunctionCallOutput;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * BaseSandboxTool adapter interface
 * Corresponds to the tool adaptation functionality in the Python version
 */
public interface ToolAdapter {
    
    /**
     * Execute tool
     */
    CompletableFuture<FunctionCallOutput> execute(FunctionCall functionCall);
    
    /**
     * Get tool schema
     */
    Map<String, Object> getSchema();
    
    /**
     * Get tool name
     */
    String getName();
    
    /**
     * Get tool description
     */
    String getDescription();
    
    /**
     * Validate parameters
     */
    boolean validateParameters(Map<String, Object> parameters);
}
