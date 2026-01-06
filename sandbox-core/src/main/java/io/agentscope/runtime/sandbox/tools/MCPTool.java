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

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MCPTool extends SandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(MCPTool.class);
    
    private Map<String, Object> serverConfigs;
    private final String mcpToolName;
    private final String sandboxType;

    public MCPTool(String name, String toolType, String description, 
                   Map<String, Object> schema, Map<String, Object> serverConfigs,
                   String sandboxType) {
        super(name, toolType, description);
        this.mcpToolName = name;
        this.schema = schema;
        this.serverConfigs = serverConfigs;
        this.sandboxType = sandboxType;
    }

    public MCPTool(String name, String toolType, String description,
                   Map<String, Object> schema, Map<String, Object> serverConfigs,
                   String sandboxType, SandboxService sandboxService) {
        super(name, toolType, description, sandboxService);
        this.mcpToolName = name;
        this.schema = schema;
        this.serverConfigs = serverConfigs;
        this.sandboxType = sandboxType;
    }
    
    public Map<String, Object> getServerConfigs() {
        return serverConfigs;
    }
    
    public void setServerConfigs(Map<String, Object> serverConfigs) {
        if (serverConfigs == null || !serverConfigs.containsKey("mcpServers")) {
            throw new IllegalArgumentException(
                "MCP server configuration must be a valid dictionary containing 'mcpServers'."
            );
        }
        this.serverConfigs = serverConfigs;
    }

    public String executeMCPTool(Map<String, Object> arguments) {
        logger.info("Executing MCP tool '{}' with arguments: {}", mcpToolName, arguments);

        if (sandbox == null) {
            throw new RuntimeException("Sandbox is not properly initialized for MCP tool");
        }

        sandbox.addMcpServers(serverConfigs, false);

        String result = sandbox.callTool(mcpToolName, arguments);

        logger.info("MCP tool '{}' execution result: {}", mcpToolName, result);
        return result;
    }

    @Override
    public Class<? extends Sandbox> getSandboxClass() {
        return BaseSandbox.class;
    }

    @Override
    public MCPTool bind(Sandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("The provided sandbox cannot be null.");
        }
        
        if (!sandboxType.equals(sandbox.getSandboxType())) {
            throw new IllegalArgumentException(
                String.format("Sandbox type mismatch! The tool requires a sandbox of type '%s', " +
                            "but a sandbox of type '%s' was provided.",
                            sandboxType, sandbox.getSandboxType())
            );
        }

        this.sandbox = sandbox;
        return this;
    }

}

