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
package io.agentscope.runtime.sandbox.manager.model.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SandboxConfig {
    private final String imageName;
    private final String sandboxType;
    private final Map<String, Object> resourceLimits;
    private final String securityLevel;
    private final int timeout;
    private final String description;
    private final Map<String, String> environment;
    private final Map<String, Object> runtimeConfig;

    private SandboxConfig(Builder builder) {
        this.imageName = builder.imageName;
        this.sandboxType = builder.sandboxType;
        this.resourceLimits = builder.resourceLimits != null ? new HashMap<>(builder.resourceLimits) : new HashMap<>();
        this.securityLevel = builder.securityLevel;
        this.timeout = builder.timeout;
        this.description = builder.description;
        this.environment = builder.environment != null ? new HashMap<>(builder.environment) : new HashMap<>();

        this.runtimeConfig = builder.runtimeConfig != null ? new HashMap<>(builder.runtimeConfig) : new HashMap<>();

        if (this.resourceLimits.containsKey("memory")) {
            this.runtimeConfig.put("mem_limit", this.resourceLimits.get("memory"));
        }
        if (this.resourceLimits.containsKey("cpu")) {
            Object cpuValue = this.resourceLimits.get("cpu");
            long nanoCpus;
            if (cpuValue instanceof Number) {
                nanoCpus = (long) (((Number) cpuValue).doubleValue() * 1_000_000_000);
            } else {
                nanoCpus = (long) (Double.parseDouble(cpuValue.toString()) * 1_000_000_000);
            }
            this.runtimeConfig.put("nano_cpus", nanoCpus);
        }
    }

    public String getImageName() {
        return imageName;
    }

    public String getSandboxType() {
        return sandboxType;
    }

    public Map<String, Object> getResourceLimits() {
        return new HashMap<>(resourceLimits);
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getEnvironment() {
        return new HashMap<>(environment);
    }

    public Map<String, Object> getRuntimeConfig() {
        return new HashMap<>(runtimeConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SandboxConfig that = (SandboxConfig) o;
        return timeout == that.timeout && Objects.equals(imageName, that.imageName) && sandboxType == that.sandboxType && Objects.equals(securityLevel, that.securityLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, sandboxType, securityLevel, timeout);
    }

    @Override
    public String toString() {
        return "SandboxConfig{" + "imageName='" + imageName + '\'' + ", sandboxType=" + sandboxType + ", resourceLimits=" + resourceLimits + ", securityLevel='" + securityLevel + '\'' + ", timeout=" + timeout + ", description='" + description + '\'' + ", environment=" + environment + ", runtimeConfig=" + runtimeConfig + '}';
    }

    /**
     * Builder for SandboxConfig
     */
    public static class Builder {
        private String imageName;
        private String sandboxType;
        private Map<String, Object> resourceLimits;
        private String securityLevel = "medium";
        private int timeout = 300;
        private String description = "";
        private Map<String, String> environment;
        private Map<String, Object> runtimeConfig;

        public Builder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder sandboxType(String sandboxType) {
            this.sandboxType = sandboxType;
            return this;
        }

        public Builder resourceLimits(Map<String, Object> resourceLimits) {
            this.resourceLimits = resourceLimits;
            return this;
        }

        public Builder securityLevel(String securityLevel) {
            this.securityLevel = securityLevel;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public Builder runtimeConfig(Map<String, Object> runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
            return this;
        }

        public SandboxConfig build() {
            Objects.requireNonNull(imageName, "imageName cannot be null");
            Objects.requireNonNull(sandboxType, "sandboxType cannot be null");
            return new SandboxConfig(this);
        }
    }
}

