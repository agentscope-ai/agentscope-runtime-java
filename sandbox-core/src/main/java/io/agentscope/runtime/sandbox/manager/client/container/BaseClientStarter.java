package io.agentscope.runtime.sandbox.manager.client.container;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;

public abstract class BaseClientStarter {
    private final ContainerClientType containerClientType;
    
    public ContainerClientType getContainerClientType() {
        return containerClientType;
    }
    
    public BaseClientStarter(ContainerClientType containerClientType){
        this.containerClientType = containerClientType;
    }

    public abstract BaseClient startClient(PortManager portManager);
}
