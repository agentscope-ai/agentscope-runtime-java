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
package io.agentscope.runtime.sandbox.tools;

import org.springframework.ai.tool.ToolCallback;

/**
 * Abstract base class that defines common interfaces for all tools
 * Similar to Python's Tool base class
 * 
 * Main responsibilities:
 * - Define standard tool interfaces
 * - Ensure consistent behavior across different tool types
 * - Provide common functionality
 */
public abstract class Tool {
    
    protected String name;
    
    protected String toolType;
    
    protected String description;
    
    /**
     * Constructor
     * 
     * @param name Tool name
     * @param toolType Tool type
     * @param description Tool description
     */
    protected Tool(String name, String toolType, String description) {
        this.name = name;
        this.toolType = toolType;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getToolType() {
        return toolType;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Build ToolCallback
     * Each concrete tool must implement this method to create its corresponding ToolCallback
     * 
     * @return ToolCallback instance
     */
    public abstract ToolCallback buildTool();
    
    @Override
    public String toString() {
        return String.format("%s(name='%s', type='%s')", 
            this.getClass().getSimpleName(), name, toolType);
    }
}

