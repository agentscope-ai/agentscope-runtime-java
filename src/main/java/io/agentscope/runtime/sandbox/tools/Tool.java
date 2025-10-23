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

public abstract class Tool {
    
    protected String name;
    
    protected String toolType;
    
    protected String description;

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

    public abstract ToolCallback buildTool();
    
    @Override
    public String toString() {
        return String.format("%s(name='%s', type='%s')", 
            this.getClass().getSimpleName(), name, toolType);
    }
}

