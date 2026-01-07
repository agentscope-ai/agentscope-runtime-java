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
package io.agentscope.runtime.sandbox.manager.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.runtime.sandbox.manager.model.fs.FileSystemType;

import java.util.Map;

public class LocalFileSystemStarter extends FileSystemStarter {

    @JsonCreator
    public LocalFileSystemStarter(
            @JsonProperty("fileSystemType") FileSystemType fileSystemType,
            @JsonProperty("readonlyMounts") Map<String, String> readonlyMounts,
            @JsonProperty("storageFolderPath") String storageFolderPath,
            @JsonProperty("mountDir") String mountDir,
            @JsonProperty("nonCopyMount") Map<String, String> nonCopyMount
    ){
        super(fileSystemType, readonlyMounts, storageFolderPath, mountDir, nonCopyMount);
    }

    private LocalFileSystemStarter(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public StorageManager createStorageManager() {
        return new LocalStorageManager(this);
    }

    public static class Builder extends FileSystemStarter.Builder<Builder> {

        public Builder() {
            super(FileSystemType.LOCAL);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public LocalFileSystemStarter build() {
            return new LocalFileSystemStarter(this);
        }
    }
}
