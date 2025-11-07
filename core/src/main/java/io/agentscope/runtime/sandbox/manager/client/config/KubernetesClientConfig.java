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
