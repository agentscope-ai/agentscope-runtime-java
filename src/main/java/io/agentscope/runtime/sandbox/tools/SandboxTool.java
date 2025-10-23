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

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;

import java.util.Map;

/**
 * Abstract class for Sandbox tools
 * Similar to Python's SandboxTool class
 * <p>
 * Extends Tool and adds sandbox-related functionality
 */
public abstract class SandboxTool extends Tool {

    protected SandboxManager sandboxManager;

    protected Sandbox sandbox;

    protected Map<String, Object> schema;

    /**
     * Constructor
     *
     * @param name        Tool name
     * @param toolType    Tool type
     * @param description Tool description
     */
    protected SandboxTool(String name, String toolType, String description) {
        super(name, toolType, description);
        this.sandboxManager = Runner.getSandboxManager();
    }

    /**
     * Constructor with custom SandboxManager
     *
     * @param name           Tool name
     * @param toolType       Tool type
     * @param description    Tool description
     * @param sandboxManager Sandbox manager
     */
    protected SandboxTool(String name, String toolType, String description,
                          SandboxManager sandboxManager) {
        super(name, toolType, description);
        this.sandboxManager = sandboxManager;
    }

    public SandboxManager getSandboxManager() {
        return sandboxManager;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    protected void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }


    /**
     * Bind sandbox to this tool
     * Returns a new tool instance bound to a specific sandbox
     *
     * @param sandbox The sandbox to bind
     * @return New bound tool instance
     */
    public abstract SandboxTool bind(Sandbox sandbox);
}
