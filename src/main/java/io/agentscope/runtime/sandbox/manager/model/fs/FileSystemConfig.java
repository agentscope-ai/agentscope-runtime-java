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

import java.util.HashMap;
import java.util.Map;

/**
 * 文件系统配置基类
 */
public class FileSystemConfig {
    private FileSystemType fileSystemType;
    private Map<String, String> readonlyMounts;
    private String storageFolderPath;
    private String mountDir;

    protected FileSystemConfig(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    protected FileSystemConfig(Builder<?> builder) {
        this.fileSystemType = builder.fileSystemType;
        this.readonlyMounts = builder.readonlyMounts;
        this.storageFolderPath = builder.storageFolderPath;
        this.mountDir = builder.mountDir;
    }

    // Getters
    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public Map<String, String> getReadonlyMounts() {
        return readonlyMounts;
    }

    public String getStorageFolderPath() {
        return storageFolderPath;
    }

    public String getMountDir() {
        return mountDir;
    }

    /**
     * Builder基类
     */
    public static abstract class Builder<T extends Builder<T>> {
        protected FileSystemType fileSystemType;
        protected Map<String, String> readonlyMounts;
        protected String storageFolderPath = "";
        protected String mountDir = "sessions_mount_dir";


        protected Builder(FileSystemType fileSystemType) {
            this.fileSystemType = fileSystemType;
        }

        protected abstract T self();

        public T readonlyMounts(Map<String, String> readonlyMounts) {
            this.readonlyMounts = readonlyMounts;
            return self();
        }

        public T addReadonlyMount(String hostPath, String containerPath) {
            if (this.readonlyMounts == null) {
                this.readonlyMounts = new HashMap<>();
            }
            this.readonlyMounts.put(hostPath, containerPath);
            return self();
        }

        public T storageFolderPath(String storageFolderPath) {
            this.storageFolderPath = storageFolderPath;
            return self();
        }

        public T mountDir(String mountDir) {
            this.mountDir = mountDir;
            return self();
        }

        public abstract FileSystemConfig build();
    }
}
