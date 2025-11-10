/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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



