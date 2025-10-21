package io.agentscope.runtime.sandbox.manager.model.fs;

public class LocalFileSystemConfig extends FileSystemConfig {
    private String mountDir = "sessions_mount_dir";

    public LocalFileSystemConfig() {
        super(FileSystemType.LOCAL);
    }

    public LocalFileSystemConfig(String mountDir) {
        super(FileSystemType.LOCAL);
        this.mountDir = mountDir;
    }

    public String getMountDir() {
        return mountDir;
    }
}
