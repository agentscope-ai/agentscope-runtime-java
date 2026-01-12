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

package io.agentscope.runtime.sandbox.client.agentrun;

import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClient;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Configuration for AgentRun client.
 * Uses Builder pattern for convenient configuration.
 */
public class AgentRunClientStarter extends BaseClientStarter {
    Logger logger = LoggerFactory.getLogger(AgentRunClientStarter.class);

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

    private AgentRunClientStarter(Builder builder) {
        super(ContainerClientType.AGENTRUN);
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

    @Override
    public BaseClient startClient(PortManager portManager) {
        try{
            AgentRunClient agentRunClient = new AgentRunClient(this);
            agentRunClient.connect();
            logger.info("AgentRun client created");
            return agentRunClient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize agentrun client");
        }
    }

    public static class Builder {
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

        public AgentRunClientStarter build() {
            return new AgentRunClientStarter(this);
        }
    }
}
