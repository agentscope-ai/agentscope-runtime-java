package io.agentscope.runtime.sandbox.manager.fs;

import io.agentscope.runtime.sandbox.manager.model.fs.FileSystemType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class FileSystemStarter implements Serializable {
    private final FileSystemType fileSystemType;
    private Map<String, String> readonlyMounts;
    private String storageFolderPath;
    private String mountDir;
    private Map<String, String> nonCopyMount;

    protected FileSystemStarter(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public abstract StorageManager createStorageManager();

    protected FileSystemStarter(Builder<?> builder) {
        this.fileSystemType = builder.fileSystemType;
        this.readonlyMounts = builder.readonlyMounts;
        this.storageFolderPath = builder.storageFolderPath;
        this.mountDir = builder.mountDir;
        this.nonCopyMount = builder.nonCopyMounts;
    }

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

    public Map<String, String> getNonCopyMount() {
        return nonCopyMount;
    }

    public static abstract class Builder<T extends Builder<T>> {
        protected FileSystemType fileSystemType;
        protected Map<String, String> readonlyMounts;
        protected String storageFolderPath = "";
        protected String mountDir = "sessions_mount_dir";
        protected Map<String, String> nonCopyMounts;

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

        public T addNonCopyMount(String hostPath, String containerPath) {
            if (this.nonCopyMounts == null) {
                this.nonCopyMounts = new HashMap<>();
            }
            this.nonCopyMounts.put(hostPath, containerPath);
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

        public abstract FileSystemStarter build();
    }
}
