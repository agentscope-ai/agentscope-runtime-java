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

package io.agentscope.runtime.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.manager.utils.InMemorySandboxMap;
import io.agentscope.runtime.sandbox.manager.utils.SandboxMap;

public class ManagerConfig {
    private final String containerPrefixKey;
    private final PortRange portRange;
    private final BaseClientStarter clientStarter;
    private final SandboxMap sandboxMap;  // SandboxKey -> ContainerModel, managed in SandboxService

    private final String baseUrl;
    private final String bearerToken;

    private final String agentBayApiKey;

    private ManagerConfig(Builder builder) {
        this.containerPrefixKey = builder.containerPrefixKey;
        this.portRange = builder.portRange;
        this.clientStarter = builder.clientStarter;
        this.sandboxMap = builder.sandboxMap;
        this.baseUrl = builder.baseUrl;
        this.bearerToken = builder.bearerToken;
        this.agentBayApiKey = builder.agentBayApiKey;
    }

    public String getContainerPrefixKey() {
        return containerPrefixKey;
    }

    public PortRange getPortRange() {
        return portRange;
    }

    public BaseClientStarter getClientStarter() {
        return clientStarter;
    }

    public SandboxMap getSandboxMap() {
        return sandboxMap;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getAgentBayApiKey() {
        return agentBayApiKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String containerPrefixKey = "sandbox_container_";
        private PortRange portRange = new PortRange(49152, 59152);
        ;
        private BaseClientStarter clientStarter = DockerClientStarter.builder().build();
        private SandboxMap sandboxMap = new InMemorySandboxMap();
        private String baseUrl;
        private String bearerToken;

        private String agentBayApiKey;

        public Builder containerPrefixKey(String containerPrefixKey) {
            this.containerPrefixKey = containerPrefixKey;
            return this;
        }

        public Builder portRange(PortRange portRange) {
            this.portRange = portRange;
            return this;
        }

        public Builder clientStarter(BaseClientStarter clientStarter) {
            this.clientStarter = clientStarter;
            return this;
        }

        public Builder sandboxMap(SandboxMap sandboxMap) {
            this.sandboxMap = sandboxMap;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder bearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder agentBayApiKey(String agentBayApiKey) {
            this.agentBayApiKey = agentBayApiKey;
            return this;
        }

        public ManagerConfig build() {
            return new ManagerConfig(this);
        }
    }

}
