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
package io.agentscope.runtime.sandbox.manager.model.container;

import java.util.HashMap;
import java.util.Map;

/**
 * Docker container properties configuration class
 */
public class DockerProp {
    private String imageName;
    private String containerName;
    private Map<Integer, Integer> portMap;
    private Map<String, String> volumeBindings;
    private Map<String, String> environment;
    private String runtimeConfig;

    public DockerProp() {
        this.portMap = new HashMap<>();
        this.volumeBindings = new HashMap<>();
        this.environment = new HashMap<>();
    }

    public DockerProp(String imageName, String containerName) {
        this();
        this.imageName = imageName;
        this.containerName = containerName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Map<Integer, Integer> getPartMap() {
        return portMap;
    }

    public void setPortMap(Map<Integer, Integer> portMap) {
        this.portMap = portMap;
    }

    public Map<String, String> getVolumeBindings() {
        return volumeBindings;
    }

    public void setVolumeBindings(Map<String, String> volumeBindings) {
        this.volumeBindings = volumeBindings;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getRuntimeConfig() {
        return runtimeConfig;
    }

    public void setRuntimeConfig(String runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public DockerProp addPortMapping(Integer containerPort, Integer hostPort) {
        this.portMap.put(containerPort, hostPort);
        return this;
    }

    public DockerProp addVolumeBinding(String hostPath, String containerPath) {
        this.volumeBindings.put(hostPath, containerPath);
        return this;
    }

    public DockerProp addEnvironment(String key, String value) {
        this.environment.put(key, value);
        return this;
    }
}
