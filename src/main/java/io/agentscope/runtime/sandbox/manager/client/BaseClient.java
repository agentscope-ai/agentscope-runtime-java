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
package io.agentscope.runtime.sandbox.manager.client;

import io.agentscope.runtime.sandbox.manager.model.VolumeBinding;

import java.util.List;
import java.util.Map;

/**
 * Container management client base class
 * Defines basic interfaces for container management, supports Docker and Kubernetes implementations
 */
public abstract class BaseClient {
    
    /**
     * Connect to container management service
     * @return connection status
     */
    public abstract boolean connect();
    
    /**
     * Create container
     * @param containerName container name
     * @param imageName image name
     * @param ports port list
     * @param portMapping port mapping
     * @param volumeBindings volume bindings
     * @param environment environment variables
     * @param runtimeConfig runtime configuration
     * @return container ID
     */
    public abstract String createContainer(String containerName, String imageName,
                                         List<String> ports, Map<String, Integer> portMapping,
                                         List<VolumeBinding> volumeBindings,
                                         Map<String, String> environment, String runtimeConfig);
    
    /**
     * Start container
     * @param containerId container ID
     */
    public abstract void startContainer(String containerId);
    
    /**
     * Stop container
     * @param containerId container ID
     */
    public abstract void stopContainer(String containerId);
    
    /**
     * Remove container
     * @param containerId container ID
     */
    public abstract void removeContainer(String containerId);
    
    /**
     * Get container status
     * @param containerId container ID
     * @return container status
     */
    public abstract String getContainerStatus(String containerId);
    
    /**
     * Stop and remove container
     * @param containerId container ID
     */
    public void stopAndRemoveContainer(String containerId) {
        stopContainer(containerId);
        removeContainer(containerId);
    }
    
    /**
     * Check connection status
     * @return whether connected
     */
    public abstract boolean isConnected();
    
    /**
     * Check if image exists locally
     * @param imageName image name
     * @return whether image exists locally
     */
    public abstract boolean imageExists(String imageName);
    
    /**
     * Pull image from registry
     * @param imageName image name
     * @return whether pull was successful
     */
    public abstract boolean pullImage(String imageName);
    
    /**
     * Ensure image is available (check and pull if needed)
     * @param imageName image name
     * @return whether image is available
     */
    public boolean ensureImageAvailable(String imageName) {
        if (imageExists(imageName)) {
            return true;
        }
        return pullImage(imageName);
    }
}
