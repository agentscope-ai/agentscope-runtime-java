package io.agentscope.runtime.sandbox.manager.model.fs;

public enum FileSystemType {
    LOCAL("local"),
    OSS("oss");

    private final String value;

    FileSystemType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FileSystemType fromString(String value) {
        for (FileSystemType type : FileSystemType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown file system type: " + value);
    }
}