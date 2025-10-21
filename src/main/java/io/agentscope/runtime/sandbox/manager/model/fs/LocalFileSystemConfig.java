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
package io.agentscope.runtime.sandbox.manager.model.fs;

/**
 * 本地文件系统配置类
 */
public class LocalFileSystemConfig extends FileSystemConfig {
    private String mountDir;

    private LocalFileSystemConfig(Builder builder) {
        super(builder);
        this.mountDir = builder.mountDir;
    }

    public String getMountDir() {
        return mountDir;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类，用于构建LocalFileSystemConfig实例
     */
    public static class Builder extends FileSystemConfig.Builder<Builder> {
        private String mountDir = "sessions_mount_dir";

        public Builder() {
            super(FileSystemType.LOCAL);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder mountDir(String mountDir) {
            this.mountDir = mountDir;
            return this;
        }

        @Override
        public LocalFileSystemConfig build() {
            return new LocalFileSystemConfig(this);
        }
    }
}
