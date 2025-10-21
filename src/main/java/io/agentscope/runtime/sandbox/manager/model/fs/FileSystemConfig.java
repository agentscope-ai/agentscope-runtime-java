package io.agentscope.runtime.sandbox.manager.model.fs;

import java.util.Map;

public class FileSystemConfig {
    private FileSystemType fileSystemType;
    private Map<String, String> readonlyMounts;
    private String storageFolderPath;

    public Map<String, String> getReadonlyMounts() {
        return readonlyMounts;
    }

    public void setReadonlyMounts(Map<String, String> readonlyMounts) {
        this.readonlyMounts = readonlyMounts;
    }

    public FileSystemConfig(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public FileSystemConfig() {
        this.fileSystemType = FileSystemType.LOCAL;
    }

    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }
}
