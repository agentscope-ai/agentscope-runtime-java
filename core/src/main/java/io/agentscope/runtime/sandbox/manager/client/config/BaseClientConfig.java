package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;

public class BaseClientConfig {
    private ContainerManagerType clientType;

    public BaseClientConfig() {
        this(ContainerManagerType.DOCKER);
    }

    public BaseClientConfig(ContainerManagerType clientType) {
        this.clientType = clientType;
    }

    public ContainerManagerType getClientType() {
        return clientType;
    }

    public void setClientType(ContainerManagerType clientType) {
        this.clientType = clientType;
    }
}
