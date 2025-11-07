package io.agentscope.maven.plugin.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Kubernetes deployment configuration
 */
public class K8sConfig {
    private String k8sNamespace = "agentscope-runtime";
    private String kubeconfigPath;
    private int replicas = 1;
    private Map<String, String> runtimeConfig = new HashMap<>();

    public String getK8sNamespace() {
        return k8sNamespace;
    }

    public void setK8sNamespace(String k8sNamespace) {
        this.k8sNamespace = k8sNamespace;
    }

    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    public void setKubeconfigPath(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public Map<String, String> getRuntimeConfig() {
        return runtimeConfig;
    }

    public void setRuntimeConfig(Map<String, String> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }
}



