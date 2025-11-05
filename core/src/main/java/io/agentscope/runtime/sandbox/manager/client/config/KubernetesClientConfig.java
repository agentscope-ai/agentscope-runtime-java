package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;
import org.apache.commons.lang3.StringUtils;

public class KubernetesClientConfig extends BaseClientConfig {
    private static final String KUBE_CONFIG_PATH = System.getProperty("user.home") + "/.kube/config";
    private String kubeConfigPath;
    private String namespace;

    private KubernetesClientConfig(){
        super(ContainerManagerType.KUBERNETES);
    }

    private KubernetesClientConfig(String kubeConfigPath, String namespace) {
        super(ContainerManagerType.KUBERNETES);
        this.kubeConfigPath = StringUtils.isEmpty(kubeConfigPath) ? KUBE_CONFIG_PATH : kubeConfigPath;
        this.namespace = namespace;
    }

    public static Builder builder() {
        return new Builder();
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

    public static class Builder {
        private String kubeConfigPath;
        private String namespace = "default";

        private Builder() {
        }

        public Builder kubeConfigPath(String kubeConfigPath) {
            this.kubeConfigPath = kubeConfigPath;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public KubernetesClientConfig build() {
            return new KubernetesClientConfig(kubeConfigPath, namespace);
        }
    }
}
