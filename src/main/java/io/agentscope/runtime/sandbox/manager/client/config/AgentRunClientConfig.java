package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;

import java.util.List;

/**
 * Configuration for AgentRun client.
 * Uses Builder pattern for convenient configuration.
 */
public class AgentRunClientConfig extends BaseClientConfig {

    private final String agentRunAccessKeyId;
    private final String agentRunAccessKeySecret;
    private final String agentRunAccountId;
    private final String agentRunRegionId;
    private final float agentRunCpu;
    private final int agentRunMemory;
    private final String agentRunVpcId;
    private final List<String> agentRunVswitchIds;
    private final String agentRunSecurityGroupId;
    private final String agentRunPrefix;
    private final String agentrunLogProject;
    private final String agentrunLogStore;

    private AgentRunClientConfig(Builder builder) {
        super(builder.isLocal, ContainerManagerType.AGENTRUN);
        this.agentRunAccessKeyId = builder.agentRunAccessKeyId;
        this.agentRunAccessKeySecret = builder.agentRunAccessKeySecret;
        this.agentRunAccountId = builder.agentRunAccountId;
        this.agentRunRegionId = builder.agentRunRegionId;
        this.agentRunCpu = builder.agentRunCpu;
        this.agentRunMemory = builder.agentRunMemory;
        this.agentRunVpcId = builder.agentRunVpcId;
        this.agentRunVswitchIds = builder.agentRunVswitchIds;
        this.agentRunSecurityGroupId = builder.agentRunSecurityGroupId;
        this.agentRunPrefix = builder.agentRunPrefix;
        this.agentrunLogProject = builder.agentrunLogProject;
        this.agentrunLogStore = builder.agentrunLogStore;
    }

    public String getAgentRunAccessKeyId() {
        return agentRunAccessKeyId;
    }

    public String getAgentRunAccessKeySecret() {
        return agentRunAccessKeySecret;
    }

    public String getAgentRunAccountId() {
        return agentRunAccountId;
    }

    public String getAgentRunRegionId() {
        return agentRunRegionId;
    }

    public float getAgentRunCpu() {
        return agentRunCpu;
    }

    public int getAgentRunMemory() {
        return agentRunMemory;
    }

    public String getAgentRunVpcId() {
        return agentRunVpcId;
    }

    public List<String> getAgentRunVswitchIds() {
        return agentRunVswitchIds;
    }

    public String getAgentRunSecurityGroupId() {
        return agentRunSecurityGroupId;
    }

    public String getAgentRunPrefix() {
        return agentRunPrefix;
    }

    public String getAgentrunLogProject() {
        return agentrunLogProject;
    }

    public String getAgentrunLogStore() {
        return agentrunLogStore;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean isLocal = true;
        private String agentRunAccessKeyId;
        private String agentRunAccessKeySecret;
        private String agentRunAccountId;
        private String agentRunRegionId = "cn-hangzhou";
        private float agentRunCpu = 2.0f;
        private int agentRunMemory = 2048;
        private String agentRunVpcId;
        private List<String> agentRunVswitchIds;
        private String agentRunSecurityGroupId;
        private String agentRunPrefix = "agentscope-sandbox_";
        private String agentrunLogProject;
        private String agentrunLogStore;

        public Builder isLocal(boolean isLocal) {
            this.isLocal = isLocal;
            return this;
        }

        public Builder agentRunAccessKeyId(String agentRunAccessKeyId) {
            this.agentRunAccessKeyId = agentRunAccessKeyId;
            return this;
        }

        public Builder agentRunAccessKeySecret(String agentRunAccessKeySecret) {
            this.agentRunAccessKeySecret = agentRunAccessKeySecret;
            return this;
        }

        public Builder agentRunAccountId(String agentRunAccountId) {
            this.agentRunAccountId = agentRunAccountId;
            return this;
        }

        public Builder agentRunRegionId(String agentRunRegionId) {
            this.agentRunRegionId = agentRunRegionId;
            return this;
        }

        public Builder agentRunCpu(float agentRunCpu) {
            this.agentRunCpu = agentRunCpu;
            return this;
        }

        public Builder agentRunMemory(int agentRunMemory) {
            this.agentRunMemory = agentRunMemory;
            return this;
        }

        public Builder agentRunVpcId(String agentRunVpcId) {
            this.agentRunVpcId = agentRunVpcId;
            return this;
        }

        public Builder agentRunVswitchIds(List<String> agentRunVswitchIds) {
            this.agentRunVswitchIds = agentRunVswitchIds;
            return this;
        }

        public Builder agentRunSecurityGroupId(String agentRunSecurityGroupId) {
            this.agentRunSecurityGroupId = agentRunSecurityGroupId;
            return this;
        }

        public Builder agentRunPrefix(String agentRunPrefix) {
            this.agentRunPrefix = agentRunPrefix;
            return this;
        }

        public Builder agentrunLogProject(String agentrunLogProject) {
            this.agentrunLogProject = agentrunLogProject;
            return this;
        }

        public Builder agentrunLogStore(String agentrunLogStore) {
            this.agentrunLogStore = agentrunLogStore;
            return this;
        }

        public AgentRunClientConfig build() {
            return new AgentRunClientConfig(this);
        }
    }
}
