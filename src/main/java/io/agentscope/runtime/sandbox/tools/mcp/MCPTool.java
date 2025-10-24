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
package io.agentscope.runtime.sandbox.tools.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class MCPTool extends SandboxTool {
    
    private static final Logger logger = Logger.getLogger(MCPTool.class.getName());
    
    private Map<String, Object> serverConfigs;
    private final String mcpToolName;
    private final SandboxType sandboxType;

    public MCPTool(String name, String toolType, String description, 
                   Map<String, Object> schema, Map<String, Object> serverConfigs,
                   SandboxType sandboxType) {
        super(name, toolType, description);
        this.mcpToolName = name;
        this.schema = schema;
        this.serverConfigs = serverConfigs;
        this.sandboxType = sandboxType;
    }

    public MCPTool(String name, String toolType, String description,
                   Map<String, Object> schema, Map<String, Object> serverConfigs,
                   SandboxType sandboxType, SandboxManager sandboxManager) {
        super(name, toolType, description, sandboxManager);
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
        
        MCPTool newTool = new MCPTool(
            this.name,
            this.toolType,
            this.description,
            this.schema,
            this.serverConfigs,
            this.sandboxType,
            this.sandboxManager
        );
        newTool.setSandbox(sandbox);
        return newTool;
    }
    
    @Override
    public ToolCallback buildTool() {
        ObjectMapper mapper = new ObjectMapper();
        String inputSchema = "";
        try {
            inputSchema = mapper.writeValueAsString(schema);
        } catch (Exception e) {
            logger.severe("Failed to serialize schema: " + e.getMessage());
            e.printStackTrace();
        }
        
        return FunctionToolCallback
                .builder(
                    name,
                    new MCPToolExecutor()
                )
                .description(description)
                .inputSchema(inputSchema)
                .inputType(Map.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    class MCPToolExecutor implements BiFunction<Map<String, Object>, ToolContext, MCPToolResponse> {
        
        @Override
        public MCPToolResponse apply(Map<String, Object> arguments, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            try {
                String result = executeMCPTool(arguments, userID, sessionID);
                return new MCPToolResponse(result, "MCP tool execution completed");
            } catch (Exception e) {
                logger.severe("MCP tool execution error: " + e.getMessage());
                e.printStackTrace();
                return new MCPToolResponse(
                    "Error",
                    "MCP tool execution error: " + e.getMessage()
                );
            }
        }
        
        private String executeMCPTool(Map<String, Object> arguments, String userID, String sessionID) {
            logger.info(String.format("Executing MCP tool '%s' with arguments: %s", 
                                     mcpToolName, arguments));
            
            if (sandbox == null) {
                sandbox = createSandbox(userID, sessionID);
            }
            
            sandbox.addMcpServers(serverConfigs, false);
            
            String result = sandbox.callTool(mcpToolName, arguments);
            
            logger.info(String.format("MCP tool '%s' execution result: %s", mcpToolName, result));
            return result;
        }
        
        private Sandbox createSandbox(String userID, String sessionID) {
            try {
                return SandboxFactory.createSandbox(sandboxType, sandboxManager, userID, sessionID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create sandbox for MCP tool", e);
            }
        }
    }


    public record MCPToolResponse(String result, String message) {
        public MCPToolResponse(String result, String message) {
            this.result = result;
            this.message = message;
        }
    }

    private static class SandboxFactory {
        static Sandbox createSandbox(SandboxType type, SandboxManager manager, 
                                    String userID, String sessionID) {
            return switch (type) {
                case BASE -> createBaseSandbox(manager, userID, sessionID);
                case BROWSER -> createBrowserSandbox(manager, userID, sessionID);
                case FILESYSTEM -> createFilesystemSandbox(manager, userID, sessionID);
                default -> throw new IllegalArgumentException("Unsupported sandbox type: " + type);
            };
        }
        
        private static Sandbox createBaseSandbox(SandboxManager manager, String userID, String sessionID) {
            try {
                Class<?> cls = Class.forName("io.agentscope.runtime.sandbox.box.BaseSandbox");
                return (Sandbox) cls
                    .getConstructor(SandboxManager.class, String.class, String.class)
                    .newInstance(manager, userID, sessionID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create BaseSandbox", e);
            }
        }
        
        private static Sandbox createBrowserSandbox(SandboxManager manager, String userID, String sessionID) {
            try {
                Class<?> cls = Class.forName("io.agentscope.runtime.sandbox.box.BrowserSandbox");
                return (Sandbox) cls
                    .getConstructor(SandboxManager.class, String.class, String.class)
                    .newInstance(manager, userID, sessionID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create BrowserSandbox", e);
            }
        }
        
        private static Sandbox createFilesystemSandbox(SandboxManager manager, String userID, String sessionID) {
            try {
                Class<?> cls = Class.forName("io.agentscope.runtime.sandbox.box.FilesystemSandbox");
                return (Sandbox) cls
                    .getConstructor(SandboxManager.class, String.class, String.class)
                    .newInstance(manager, userID, sessionID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create FilesystemSandbox", e);
            }
        }
    }
}

