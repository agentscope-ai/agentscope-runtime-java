package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;
import org.apache.commons.lang3.StringUtils;

public class KubernetesClientConfig extends BaseClientConfig {
    private static final String KUBE_CONFIG_PATH = System.getProperty("user.home") + "/.kube/config";
    private String kubeConfigPath;
    private String namespace;

    public KubernetesClientConfig(){
        this(null, "default");
    }

    public KubernetesClientConfig(String kubeConfigPath) {
        this(kubeConfigPath, "default");
    }

    public KubernetesClientConfig(String kubeConfigPath, String namespace) {
        super(ContainerManagerType.KUBERNETES);
        this.kubeConfigPath = StringUtils.isEmpty(kubeConfigPath) ? KUBE_CONFIG_PATH : kubeConfigPath;
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
