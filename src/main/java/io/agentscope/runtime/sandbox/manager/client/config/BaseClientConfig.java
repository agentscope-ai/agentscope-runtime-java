package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.ContainerManagerType;

public class BaseClientConfig {
    private boolean isLocal;
    private ContainerManagerType clientType;

    public BaseClientConfig() {
        this(true, ContainerManagerType.DOCKER);
    }

    public BaseClientConfig(boolean isLocal) {
        this(isLocal, ContainerManagerType.DOCKER);
    }

    public BaseClientConfig(boolean isLocal, ContainerManagerType clientType) {
        this.isLocal = isLocal;
        this.clientType = clientType;
    }

    public boolean getIsLocal() {
        return isLocal;
    }

    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public ContainerManagerType getClientType() {
        return clientType;
    }

    public void setClientType(ContainerManagerType clientType) {
        this.clientType = clientType;
    }
}
