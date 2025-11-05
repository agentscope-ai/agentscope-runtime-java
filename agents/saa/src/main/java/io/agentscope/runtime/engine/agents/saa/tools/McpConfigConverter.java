package io.agentscope.runtime.engine.agents.saa.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;

public class McpConfigConverter {

    private static final Logger logger = Logger.getLogger(McpConfigConverter.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> serverConfigs;
    private Set<String> whitelist;
    private Set<String> blacklist;
    private SandboxManager sandboxManager;
    private SandboxType sandboxType;
    private Sandbox sandbox = null;

    public McpConfigConverter(Map<String, Object> serverConfigs, SandboxType sandboxType,
                              Set<String> whitelist, Set<String> blacklist) {
        this(serverConfigs, sandboxType, whitelist, blacklist, null);
    }

    public McpConfigConverter(Map<String, Object> serverConfigs, SandboxType sandboxType,
                              Set<String> whitelist, Set<String> blacklist,
                              SandboxManager sandboxManager) {
        this(serverConfigs, sandboxType, whitelist, blacklist, sandboxManager, null);
    }

    public McpConfigConverter(Map<String, Object> serverConfigs, SandboxType sandboxType,
                              Set<String> whitelist, Set<String> blacklist,
                              SandboxManager sandboxManager, Sandbox sandbox) {
        this.serverConfigs = serverConfigs;
        this.whitelist = whitelist != null ? whitelist : new HashSet<>();
        this.blacklist = blacklist != null ? blacklist : new HashSet<>();
        this.sandboxManager = sandboxManager;
        this.sandboxType = sandboxType;
        this.sandbox = sandbox;

        if (!serverConfigs.containsKey("mcpServers")) {
            throw new IllegalArgumentException("MCP server config must contain 'mcpServers'");
        }
    }

    public McpConfigConverter bind(Sandbox sandbox) {
        return new McpConfigConverter(
                new HashMap<>(this.serverConfigs),
                sandboxType,
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

    public List<MCPTool> toBuiltinTools() {
        Sandbox box = this.sandbox;
        List<MCPTool> toolsToAdd = new ArrayList<>();
        if(box == null){
            try(BaseSandbox baseSandbox = new BaseSandbox(sandboxManager, "", "")){
                box = baseSandbox;
                toolsToAdd = processTools(box);
            }
            catch (Exception e){
                logger.severe("Failed to create BaseSandbox: " + e.getMessage());
                throw new RuntimeException("Failed to create BaseSandbox", e);
            }
        }
        else{
            toolsToAdd = processTools(box);
            for (MCPTool tool : toolsToAdd) {
                tool.bind(box);
            }
        }
        return toolsToAdd;
    }

    @SuppressWarnings("unchecked")
    private List<MCPTool> processTools(Sandbox box) {
        List<MCPTool> toolsToAdd = new ArrayList<>();

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
                        sandboxType,
                        sandboxManager
                );

                toolsToAdd.add(mcpTool);
                logger.info(String.format("Added MCP tool: %s (server: %s)", toolName, serverName));
            }
        }

        logger.info(String.format("Total MCP tools added: %d", toolsToAdd.size()));

        return toolsToAdd;
    }

    private static Map<String, Object> parseServerConfig(String configStr) {
        try {
            return objectMapper.readValue(configStr, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.severe("Failed to parse server config: " + e.getMessage());
            throw new RuntimeException("Failed to parse server config string", e);
        }
    }

    public static McpConfigConverter fromDict(Map<String, Object> configDict,
                                              Set<String> whitelist,
                                              Set<String> blacklist) {
        return new McpConfigConverter(configDict, null, whitelist, blacklist);
    }

    public static McpConfigConverter fromDict(Map<String, Object> configDict) {
        return fromDict(configDict, null, null);
    }

    public static McpConfigConverter fromString(String configStr,
                                                Set<String> whitelist,
                                                Set<String> blacklist) {
        Map<String, Object> configDict = parseServerConfig(configStr);
        return new McpConfigConverter(configDict, null, whitelist, blacklist);
    }

    public static McpConfigConverter fromString(String configStr) {
        return fromString(configStr, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, Object> serverConfigs;
        private SandboxType sandboxType = SandboxType.BASE;
        private Set<String> whitelist;
        private Set<String> blacklist;
        private SandboxManager sandboxManager;

        public Builder serverConfigs(Map<String, Object> serverConfigs) {
            this.serverConfigs = serverConfigs;
            return this;
        }

        public Builder serverConfigs(String serverConfigsStr) {
            this.serverConfigs = parseServerConfig(serverConfigsStr);
            return this;
        }

        public Builder sandboxType(SandboxType sandboxType) {
            this.sandboxType = sandboxType;
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
            return new McpConfigConverter(serverConfigs, sandboxType, whitelist, blacklist, sandboxManager);
        }
    }
}
