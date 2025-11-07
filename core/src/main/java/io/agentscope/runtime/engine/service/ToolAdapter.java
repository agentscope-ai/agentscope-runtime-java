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
