package io.agentscope.runtime.sandbox.tools.utils;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.mcp.MCPTool;

import java.util.*;
import java.util.logging.Logger;

public class McpConfigConverter {
    
    private static final Logger logger = Logger.getLogger(McpConfigConverter.class.getName());
    
    private Map<String, Object> serverConfigs;
    private Sandbox sandbox;
    private Set<String> whitelist;
    private Set<String> blacklist;
    private SandboxManager sandboxManager;

    public McpConfigConverter(Map<String, Object> serverConfigs, Sandbox sandbox, 
                            Set<String> whitelist, Set<String> blacklist) {
        this(serverConfigs, sandbox, whitelist, blacklist, null);
    }

    public McpConfigConverter(Map<String, Object> serverConfigs, Sandbox sandbox,
                            Set<String> whitelist, Set<String> blacklist,
                            SandboxManager sandboxManager) {
        this.serverConfigs = serverConfigs;
        this.sandbox = sandbox;
        this.whitelist = whitelist != null ? whitelist : new HashSet<>();
        this.blacklist = blacklist != null ? blacklist : new HashSet<>();
        this.sandboxManager = sandboxManager;

        if(!serverConfigs.containsKey("mcpServers")){
            throw new IllegalArgumentException("MCP server config must contain 'mcpServers'");
        }
    }

    public McpConfigConverter bind(Sandbox sandbox) {
        return new McpConfigConverter(
            new HashMap<>(this.serverConfigs), 
            sandbox, 
            new HashSet<>(this.whitelist), 
            new HashSet<>(this.blacklist),
            this.sandboxManager
        );
    }

    public Map<String, Object> getServerConfigs() {
        return serverConfigs;
    }

    public void setServerConfigs(Map<String, Object> serverConfigs) {
        this.serverConfigs = serverConfigs;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(Set<String> whitelist) {
        this.whitelist = whitelist;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    public SandboxManager getSandboxManager() {
        return sandboxManager;
    }

    public void setSandboxManager(SandboxManager sandboxManager) {
        this.sandboxManager = sandboxManager;
    }

    public List<MCPTool> toBuiltinTools(Sandbox sandbox) {
        Sandbox box = sandbox != null ? sandbox : this.sandbox;
        
        if (box == null) {
            // Create a temporary base sandbox for tool discovery
            logger.info("No sandbox provided, creating temporary sandbox for MCP tool discovery");
            try {
                // Use BaseSandbox with a temporary user/session ID
                BaseSandbox tempBox = new BaseSandbox(
                    sandboxManager, 
                    "temp_user", 
                    "temp_session"
                );
                try {
                    return processTools(tempBox);
                } finally {
                    tempBox.close();
                }
            } catch (Exception e) {
                logger.severe("Failed to create temporary sandbox: " + e.getMessage());
                throw new RuntimeException("Failed to create temporary sandbox for MCP tools", e);
            }
        } else {
            return processTools(box);
        }
    }

    public List<MCPTool> toBuiltinTools() {
        return toBuiltinTools(null);
    }

    @SuppressWarnings("unchecked")
    private List<MCPTool> processTools(Sandbox box) {
        List<MCPTool> toolsToAdd = new ArrayList<>();
        
        try {
            box.addMcpServers(serverConfigs, false);
            
            Map<String, Object> mcpServers = (Map<String, Object>) serverConfigs.get("mcpServers");
            
            for (String serverName : mcpServers.keySet()) {
                logger.info("Processing MCP server: " + serverName);
                
                Map<String, Object> tools = box.listTools(serverName);
                
                Map<String, Object> serverTools = (Map<String, Object>) tools.getOrDefault(serverName, new HashMap<>());
                
                for (Map.Entry<String, Object> toolEntry : serverTools.entrySet()) {
                    String toolName = toolEntry.getKey();
                    Map<String, Object> toolInfo = (Map<String, Object>) toolEntry.getValue();
                    
                    if (!whitelist.isEmpty() && !whitelist.contains(toolName)) {
                        logger.fine("Skipping tool (not in whitelist): " + toolName);
                        continue;
                    }
                    if (!blacklist.isEmpty() && blacklist.contains(toolName)) {
                        logger.fine("Skipping tool (in blacklist): " + toolName);
                        continue;
                    }
                    
                    Map<String, Object> jsonSchema = (Map<String, Object>) toolInfo.get("json_schema");
                    Map<String, Object> functionSchema = (Map<String, Object>) jsonSchema.get("function");
                    
                    String description = (String) functionSchema.getOrDefault("description", "");
                    
                    Map<String, Object> toolServerConfig = new HashMap<>();
                    Map<String, Object> toolMcpServers = new HashMap<>();
                    toolMcpServers.put(serverName, mcpServers.get(serverName));
                    toolServerConfig.put("mcpServers", toolMcpServers);
                    
                    MCPTool mcpTool = new MCPTool(
                        toolName,
                        serverName,
                        description,
                        functionSchema,
                        toolServerConfig,
                        box.getSandboxType(),
                        sandboxManager
                    );
                    
                    mcpTool = mcpTool.bind(box);
                    
                    toolsToAdd.add(mcpTool);
                    logger.info(String.format("Added MCP tool: %s (server: %s)", toolName, serverName));
                }
            }
            
            logger.info(String.format("Total MCP tools added: %d", toolsToAdd.size()));
            
        } catch (Exception e) {
            logger.severe("Error processing MCP tools: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process MCP tools", e);
        }
        
        return toolsToAdd;
    }

    public static McpConfigConverter fromDict(Map<String, Object> configDict,
                                              Set<String> whitelist,
                                              Set<String> blacklist) {
        return new McpConfigConverter(configDict, null, whitelist, blacklist);
    }

    public static McpConfigConverter fromDict(Map<String, Object> configDict) {
        return fromDict(configDict, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, Object> serverConfigs;
        private Sandbox sandbox;
        private Set<String> whitelist;
        private Set<String> blacklist;
        private SandboxManager sandboxManager;

        public Builder serverConfigs(Map<String, Object> serverConfigs) {
            this.serverConfigs = serverConfigs;
            return this;
        }

        public Builder sandbox(Sandbox sandbox) {
            this.sandbox = sandbox;
            return this;
        }

        public Builder whitelist(Set<String> whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        public Builder blacklist(Set<String> blacklist) {
            this.blacklist = blacklist;
            return this;
        }
        
        public Builder sandboxManager(SandboxManager sandboxManager) {
            this.sandboxManager = sandboxManager;
            return this;
        }

        public McpConfigConverter build() {
            return new McpConfigConverter(serverConfigs, sandbox, whitelist, blacklist, sandboxManager);
        }
    }
}
