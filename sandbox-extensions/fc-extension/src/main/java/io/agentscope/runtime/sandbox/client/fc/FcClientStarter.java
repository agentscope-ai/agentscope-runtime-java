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

package io.agentscope.runtime.sandbox.client.fc;

import io.agentscope.runtime.sandbox.manager.client.container.BaseClient;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FcClientStarter extends BaseClientStarter {
    Logger logger = LoggerFactory.getLogger(FcClientStarter.class);
    private final String FcAccessKeyId;
    private final String FcAccessKeySecret;
    private final String FcAccountId;
    private final String FcRegionId;
    private final float FcCpu;
    private final int FcMemory;
    private final String FcVpcId;
    private final List<String> FcVswitchIds;
    private final String FcSecurityGroupId;
    private final String FcPrefix;
    private final String FcLogProject;
    private final String FcLogStore;

    private FcClientStarter(Builder builder) {
        super(ContainerClientType.FC);
        this.FcAccessKeyId = builder.FcAccessKeyId;
        this.FcAccessKeySecret = builder.FcAccessKeySecret;
        this.FcAccountId = builder.FcAccountId;
        this.FcRegionId = builder.FcRegionId;
        this.FcCpu = builder.FcCpu;
        this.FcMemory = builder.FcMemory;
        this.FcVpcId = builder.FcVpcId;
        this.FcVswitchIds = builder.FcVswitchIds;
        this.FcSecurityGroupId = builder.FcSecurityGroupId;
        this.FcPrefix = builder.FcPrefix;
        this.FcLogProject = builder.FcLogProject;
        this.FcLogStore = builder.FcLogStore;
    }

    public String getFcAccessKeyId() {
        return FcAccessKeyId;
    }

    public String getFcAccessKeySecret() {
        return FcAccessKeySecret;
    }

    public String getFcAccountId() {
        return FcAccountId;
    }

    public String getFcRegionId() {
        return FcRegionId;
    }

    public float getFcCpu() {
        return FcCpu;
    }

    public int getFcMemory() {
        return FcMemory;
    }

    public String getFcVpcId() {
        return FcVpcId;
    }

    public List<String> getFcVswitchIds() {
        return FcVswitchIds;
    }

    public String getFcSecurityGroupId() {
        return FcSecurityGroupId;
    }

    public String getFcPrefix() {
        return FcPrefix;
    }

    public String getFcLogProject() {
        return FcLogProject;
    }

    public String getFcLogStore() {
        return FcLogStore;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public BaseClient startClient(PortManager portManager) {
        try{
            FcClient fcClient = new FcClient(this);
            fcClient.connect();
            logger.info("FC client created");
            return fcClient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FC client");
        }

    }

    public static class Builder {
        private String FcAccessKeyId;
        private String FcAccessKeySecret;
        private String FcAccountId;
        private String FcRegionId = "cn-hangzhou";
        private float FcCpu = 2.0f;
        private int FcMemory = 2048;
        private String FcVpcId;
        private List<String> FcVswitchIds;
        private String FcSecurityGroupId;
        private String FcPrefix = "agentscope-sandbox_";
        private String FcLogProject;
        private String FcLogStore;

        public Builder FcAccessKeyId(String FcAccessKeyId) {
            this.FcAccessKeyId = FcAccessKeyId;
            return this;
        }

        public Builder FcAccessKeySecret(String FcAccessKeySecret) {
            this.FcAccessKeySecret = FcAccessKeySecret;
            return this;
        }

        public Builder FcAccountId(String FcAccountId) {
            this.FcAccountId = FcAccountId;
            return this;
        }

        public Builder FcRegionId(String FcRegionId) {
            this.FcRegionId = FcRegionId;
            return this;
        }

        public Builder FcCpu(float FcCpu) {
            this.FcCpu = FcCpu;
            return this;
        }

        public Builder FcMemory(int FcMemory) {
            this.FcMemory = FcMemory;
            return this;
        }

        public Builder FcVpcId(String FcVpcId) {
            this.FcVpcId = FcVpcId;
            return this;
        }

        public Builder FcVswitchIds(List<String> FcVswitchIds) {
            this.FcVswitchIds = FcVswitchIds;
            return this;
        }

        public Builder FcSecurityGroupId(String FcSecurityGroupId) {
            this.FcSecurityGroupId = FcSecurityGroupId;
            return this;
        }

        public Builder FcPrefix(String FcPrefix) {
            this.FcPrefix = FcPrefix;
            return this;
        }

        public Builder FcLogProject(String FcLogProject) {
            this.FcLogProject = FcLogProject;
            return this;
        }

        public Builder FcLogStore(String FcLogStore) {
            this.FcLogStore = FcLogStore;
            return this;
        }

        public FcClientStarter build() {
            return new FcClientStarter(this);
        }
    }
}
