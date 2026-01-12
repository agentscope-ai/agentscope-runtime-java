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
package io.agentscope.runtime.sandbox.map;

/**
 * Redis Manager Configuration Class
 */
public class RedisManagerConfig {
    private final String redisServer;
    private final Integer redisPort;
    private final Integer redisDb;
    private final String redisUser;
    private final String redisPassword;

    private RedisManagerConfig(Builder builder) {
        this.redisServer = builder.redisServer;
        this.redisPort = builder.redisPort;
        this.redisDb = builder.redisDb;
        this.redisUser = builder.redisUser;
        this.redisPassword = builder.redisPassword;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String redisServer = "localhost";
        private Integer redisPort = 6379;
        private Integer redisDb = 0;
        private String redisUser;
        private String redisPassword;

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

        public RedisManagerConfig build() {
            return new RedisManagerConfig(this);
        }
    }
}



