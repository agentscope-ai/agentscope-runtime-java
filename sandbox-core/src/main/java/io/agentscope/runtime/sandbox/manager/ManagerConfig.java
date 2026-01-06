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
    private final BaseClientStarter clientConfig;
    private final SandboxMap sandboxMap;  // SandboxKey -> ContainerModel, managed in SandboxService

    private final String baseUrl;
    private final String bearerToken;

    private ManagerConfig(Builder builder) {
        this.containerPrefixKey = builder.containerPrefixKey;
        this.portRange = builder.portRange;
        this.clientConfig = builder.clientConfig;
        this.sandboxMap = builder.sandboxMap;
        this.baseUrl = builder.baseUrl;
        this.bearerToken = builder.bearerToken;
    }

    public String getContainerPrefixKey() {
        return containerPrefixKey;
    }

    public PortRange getPortRange() {
        return portRange;
    }

    public BaseClientStarter getClientConfig() {
        return clientConfig;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String containerPrefixKey = "sandbox_container_";
        private PortRange portRange = new PortRange(49152, 59152);
        ;
        private BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        private SandboxMap sandboxMap = new InMemorySandboxMap();
        private String baseUrl;
        private String bearerToken;

        public Builder containerPrefixKey(String containerPrefixKey) {
            this.containerPrefixKey = containerPrefixKey;
            return this;
        }

        public Builder portRange(PortRange portRange) {
            this.portRange = portRange;
            return this;
        }

        public Builder clientConfig(BaseClientStarter clientConfig) {
            this.clientConfig = clientConfig;
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

        public ManagerConfig build() {
            return new ManagerConfig(this);
        }
    }

}
