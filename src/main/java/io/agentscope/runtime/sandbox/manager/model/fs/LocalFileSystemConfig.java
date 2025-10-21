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
 * Local File System Configuration Class
 */
public class LocalFileSystemConfig extends FileSystemConfig {

    private LocalFileSystemConfig(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing LocalFileSystemConfig instances
     */
    public static class Builder extends FileSystemConfig.Builder<Builder> {

        public Builder() {
            super(FileSystemType.LOCAL);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public LocalFileSystemConfig build() {
            return new LocalFileSystemConfig(this);
        }
    }
}
