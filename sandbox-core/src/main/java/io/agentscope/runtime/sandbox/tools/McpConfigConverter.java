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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class McpConfigConverter {

    private static final Logger logger = LoggerFactory.getLogger(McpConfigConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> serverConfigs;
    private Set<String> whitelist;
    private Set<String> blacklist;
    private SandboxService sandboxService;
    private final String sandboxType;
    private Sandbox sandbox = null;

    public McpConfigConverter(Map<String, Object> serverConfigs, String sandboxType,
                              Set<String> whitelist, Set<String> blacklist) {
        this(serverConfigs, sandboxType, whitelist, blacklist, null);
    }

    public McpConfigConverter(Map<String, Object> serverConfigs, String sandboxType,
                              Set<String> whitelist, Set<String> blacklist,
                              SandboxService sandboxService) {
        this(serverConfigs, sandboxType, whitelist, blacklist, sandboxService, null);
    }

    public McpConfigConverter(Map<String, Object> serverConfigs, String sandboxType,
                              Set<String> whitelist, Set<String> blacklist,
                              SandboxService sandboxService, Sandbox sandbox) {
        this.serverConfigs = serverConfigs;
        this.whitelist = whitelist != null ? whitelist : new HashSet<>();
        this.blacklist = blacklist != null ? blacklist : new HashSet<>();
        this.sandboxService = sandboxService;
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
                this.sandboxService
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

    public SandboxService getSandboxService() {
        return sandboxService;
    }

    public void setSandboxService(SandboxService sandboxService) {
        this.sandboxService = sandboxService;
    }

    public List<MCPTool> toBuiltinTools() {
        Sandbox box = this.sandbox;
        List<MCPTool> toolsToAdd = new ArrayList<>();
        if(box == null){
            try(BaseSandbox baseSandbox = new BaseSandbox(sandboxService, "", "")){
                box = baseSandbox;
                toolsToAdd = processTools(box);
            }
            catch (Exception e){
                logger.error("Failed to create BaseSandbox: {}", e.getMessage());
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
            logger.info("Processing MCP server: {}", serverName);

            Map<String, Object> tools = box.listTools(serverName);

            Map<String, Object> serverTools = (Map<String, Object>) tools.getOrDefault(serverName, new HashMap<>());

            for (Map.Entry<String, Object> toolEntry : serverTools.entrySet()) {
                String toolName = toolEntry.getKey();
                Map<String, Object> toolInfo = (Map<String, Object>) toolEntry.getValue();

                if (!whitelist.isEmpty() && !whitelist.contains(toolName)) {
                    logger.info("Skipping tool (not in whitelist): {}", toolName);
                    continue;
                }
                if (!blacklist.isEmpty() && blacklist.contains(toolName)) {
                    logger.info("Skipping tool (in blacklist): {}", toolName);
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
                        sandboxService
                );

                toolsToAdd.add(mcpTool);
                logger.info("Added MCP tool: {} (server: {})", toolName, serverName);
            }
        }

        logger.info("Total MCP tools added: {}", toolsToAdd.size());

        return toolsToAdd;
    }

    private static Map<String, Object> parseServerConfig(String configStr) {
        try {
            return objectMapper.readValue(configStr, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.error("Failed to parse server config: {}", e.getMessage());
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
        private String sandboxType = "base";
        private Set<String> whitelist;
        private Set<String> blacklist;
        private SandboxService sandboxService;

        public Builder serverConfigs(Map<String, Object> serverConfigs) {
            this.serverConfigs = serverConfigs;
            return this;
        }

        public Builder serverConfigs(String serverConfigsStr) {
            this.serverConfigs = parseServerConfig(serverConfigsStr);
            return this;
        }

        public Builder sandboxType(String sandboxType) {
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

        public Builder sandboxService(SandboxService sandboxService) {
            this.sandboxService = sandboxService;
            return this;
        }

        public McpConfigConverter build() {
            return new McpConfigConverter(serverConfigs, sandboxType, whitelist, blacklist, sandboxService);
        }
    }
}
