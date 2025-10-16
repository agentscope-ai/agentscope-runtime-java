package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.ContainerManagerType;

public class KubernetesClientConfig extends BaseClientConfig {
    private String kubeConfigPath;
    private String namespace;

    public KubernetesClientConfig(){
        this(true, null, "default");
    }

    public KubernetesClientConfig(String kubeConfigPath) {
        this(false, kubeConfigPath, "default");
    }

    public KubernetesClientConfig(boolean isLocal) {
        this(isLocal, null, "default");
    }

    public KubernetesClientConfig(boolean isLocal, String kubeConfigPath, String namespace) {
        super(isLocal, ContainerManagerType.KUBERNETES);
        this.kubeConfigPath = kubeConfigPath;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKubeConfigPath() {
        return kubeConfigPath;
    }

    public void setKubeConfigPath(String kubeConfigPath) {
        this.kubeConfigPath = kubeConfigPath;
    }
}
