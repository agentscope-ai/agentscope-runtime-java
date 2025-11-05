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

/**
 * Redis Manager Configuration Class
 */
public class RedisManagerConfig {
    private String redisServer;
    private Integer redisPort;
    private Integer redisDb;
    private String redisUser;
    private String redisPassword;
    private String redisPortKey;
    private String redisContainerPoolKey;

    private RedisManagerConfig(Builder builder) {
        this.redisServer = builder.redisServer;
        this.redisPort = builder.redisPort;
        this.redisDb = builder.redisDb;
        this.redisUser = builder.redisUser;
        this.redisPassword = builder.redisPassword;
        this.redisPortKey = builder.redisPortKey;
        this.redisContainerPoolKey = builder.redisContainerPoolKey;
    }

    public String getRedisServer() {
        return redisServer;
    }

    public Integer getRedisPort() {
        return redisPort;
    }

    public Integer getRedisDb() {
        return redisDb;
    }

    public String getRedisUser() {
        return redisUser;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public String getRedisPortKey() {
        return redisPortKey;
    }

    public String getRedisContainerPoolKey() {
        return redisContainerPoolKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String redisServer = "localhost";
        private Integer redisPort = 6379;
        private Integer redisDb = 0;
        private String redisUser;
        private String redisPassword;
        private String redisPortKey = "_runtime_sandbox_container_occupied_ports";
        private String redisContainerPoolKey = "_runtime_sandbox_container_container_pool";

        public Builder redisServer(String redisServer) {
            this.redisServer = redisServer;
            return this;
        }

        public Builder redisPort(Integer redisPort) {
            this.redisPort = redisPort;
            return this;
        }

        public Builder redisDb(Integer redisDb) {
            this.redisDb = redisDb;
            return this;
        }

        public Builder redisUser(String redisUser) {
            this.redisUser = redisUser;
            return this;
        }

        public Builder redisPassword(String redisPassword) {
            this.redisPassword = redisPassword;
            return this;
        }

        public Builder redisPortKey(String redisPortKey) {
            this.redisPortKey = redisPortKey;
            return this;
        }

        public Builder redisContainerPoolKey(String redisContainerPoolKey) {
            this.redisContainerPoolKey = redisContainerPoolKey;
            return this;
        }

        public RedisManagerConfig build() {
            return new RedisManagerConfig(this);
        }
    }
}



