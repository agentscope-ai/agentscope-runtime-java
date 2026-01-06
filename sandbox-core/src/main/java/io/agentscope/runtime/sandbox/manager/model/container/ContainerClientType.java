package io.agentscope.runtime.sandbox.manager.model.container;

public enum ContainerClientType {
    DOCKER("docker"), KUBERNETES("kubernetes"), AGENTRUN("agentrun"), FC("fc");

    private final String value;

    ContainerClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContainerClientType fromString(String value) {
        for (ContainerClientType type : ContainerClientType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown container manager type: " + value);
    }
}
