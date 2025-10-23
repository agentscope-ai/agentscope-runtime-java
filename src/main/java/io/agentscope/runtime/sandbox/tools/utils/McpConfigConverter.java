package io.agentscope.runtime.sandbox.tools.utils;

import io.agentscope.runtime.sandbox.box.Sandbox;

import java.util.Map;
import java.util.Set;

public class McpConfigConverter {
    private Map<String, Object> serverConfigs;
    private Sandbox sandbox;
    private Set<String> whitelist;
    private Set<String> blacklist;

    public McpConfigConverter(Map<String, Object> serverConfigs, Sandbox sandbox, Set<String> whitelist, Set<String> blacklist) {
        this.serverConfigs = serverConfigs;
        this.sandbox = sandbox;
        this.whitelist = whitelist;
        this.blacklist = blacklist;

        if(!serverConfigs.containsKey("mcpServers")){
            throw new IllegalArgumentException("MCP server config must contain 'mcpServers'");
        }
    }

    public McpConfigConverter bind(Sandbox sandbox) {
        return new McpConfigConverter(this.serverConfigs, sandbox, this.whitelist, this.blacklist);
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


    public Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public Map<String, Object> serverConfigs;
        public Sandbox sandbox;
        public Set<String> whitelist;
        public Set<String> blacklist;

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

        public McpConfigConverter build() {
            return new McpConfigConverter(serverConfigs, sandbox, whitelist, blacklist);
        }
    }
}
