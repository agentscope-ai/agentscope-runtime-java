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
package io.agentscope.runtime.sandbox.manager.model;

import io.agentscope.runtime.sandbox.manager.client.config.AgentRunClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;
import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.manager.model.container.RedisManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.FileSystemType;
import io.agentscope.runtime.sandbox.manager.model.fs.LocalFileSystemConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.OssConfig;

import java.io.File;

/**
 * Sandbox Manager Environment Configuration Class
 */
public class ManagerConfig {

    private static final int UUID_LENGTH = 25;

    private String containerPrefixKey;

    private Boolean redisEnabled = false;


    private PortRange portRange;
    private int poolSize = 0;

    private BaseClientConfig clientConfig;
    private FileSystemConfig fileSystemConfig;
    private RedisManagerConfig redisConfig;

    private ManagerConfig(Builder builder) {
        this.containerPrefixKey = builder.containerPrefixKey;
        this.redisEnabled = builder.redisEnabled;
        this.redisConfig = builder.redisConfig;

        this.clientConfig = builder.clientConfig;
        this.fileSystemConfig = builder.fileSystemConfig;
        this.portRange = builder.portRange;
        this.poolSize = builder.poolSize;

        validate();
    }

    private void validate() {
        if (fileSystemConfig.getFileSystemType() == FileSystemType.LOCAL && fileSystemConfig instanceof LocalFileSystemConfig localFileSystemConfig) {
            File dir = new File(localFileSystemConfig.getMountDir());
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

        if (fileSystemConfig.getFileSystemType() == FileSystemType.OSS && fileSystemConfig instanceof OssConfig ossConfig) {
            validateRequired("ossEndpoint", ossConfig.getOssEndpoint(), "file_system is 'oss'");
            validateRequired("ossAccessKeyId", ossConfig.getOssAccessKeyId(), "file_system is 'oss'");
            validateRequired("ossAccessKeySecret", ossConfig.getOssAccessKeySecret(), "file_system is 'oss'");
            validateRequired("ossBucketName", ossConfig.getOssBucketName(), "file_system is 'oss'");
        }

        if (redisEnabled) {
            validateRequired("redisServer", redisConfig.getRedisServer(), "redis is enabled");
            validateRequired("redisPort", redisConfig.getRedisPort(), "redis is enabled");
            validateRequired("redisDb", redisConfig.getRedisDb(), "redis is enabled");
            validateRequired("redisPortKey", redisConfig.getRedisPortKey(), "redis is enabled");
            validateRequired("redisContainerPoolKey", redisConfig.getRedisContainerPoolKey(), "redis is enabled");
        }

        if (clientConfig.getClientType() == ContainerManagerType.AGENTRUN && clientConfig instanceof AgentRunClientConfig agentRunClientConfig) {
            validateRequired("agentRunAccessKeyId", agentRunClientConfig.getAgentRunAccessKeyId(), "container_deployment is 'agentrun'");
            validateRequired("agentRunAccessKeySecret", agentRunClientConfig.getAgentRunAccessKeySecret(), "container_deployment is 'agentrun'");
            validateRequired("agentRunAccountId", agentRunClientConfig.getAgentRunAccountId(), "container_deployment is 'agentrun'");
        }
    }

    private void validateRequired(String fieldName, Object value, String condition) {
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            throw new IllegalArgumentException(fieldName + " must be set when " + condition);
        }
    }

    public String getContainerPrefixKey() {
        return containerPrefixKey;
    }

    public Boolean getRedisEnabled() {
        return redisEnabled;
    }

    public BaseClientConfig getClientConfig() {
        return clientConfig;
    }

    public PortRange getPortRange() {
        return portRange;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public FileSystemConfig getFileSystemConfig() {
        return fileSystemConfig;
    }

    public RedisManagerConfig getRedisConfig() {
        return redisConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String containerPrefixKey = "runtime_sandbox_container_";
        private Boolean redisEnabled = false;
        private BaseClientConfig clientConfig = new DockerClientConfig();
        private RedisManagerConfig redisConfig;
        private PortRange portRange = new PortRange(49152, 59152);
        private int poolSize = 0;

        private FileSystemConfig fileSystemConfig = LocalFileSystemConfig.builder().build();

        public Builder containerPrefixKey(String containerPrefixKey) {
            if (containerPrefixKey.length() > 63 - UUID_LENGTH) {
                throw new IllegalArgumentException("containerPrefixKey max length is " + (63 - UUID_LENGTH));
            }
            this.containerPrefixKey = containerPrefixKey;
            return this;
        }

        public Builder storageFolder(String storageFolder) {
            return this;
        }

        public Builder redisConfig(RedisManagerConfig redisConfig) {
            this.redisConfig = redisConfig;
            this.redisEnabled = true;
            return this;
        }

        public Builder containerDeployment(BaseClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        public Builder portRange(int start, int end) {
            this.portRange = new PortRange(start, end);
            return this;
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder fileSystemConfig(FileSystemConfig fileSystemConfig) {
            this.fileSystemConfig = fileSystemConfig;
            return this;
        }

        public ManagerConfig build() {
            return new ManagerConfig(this);
        }
    }
}
