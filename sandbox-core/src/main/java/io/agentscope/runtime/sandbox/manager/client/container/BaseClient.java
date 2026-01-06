package io.agentscope.runtime.sandbox.manager.client.container;


import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;

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
     * @param volumeBindings volume bindings
     * @param environment environment variables
     * @param runtimeConfig runtime configuration
     * @return ContainerCreateResult containing containerId, ports, ip, and optional protocol
     */
    public abstract ContainerCreateResult createContainer(String containerName, String imageName,
                                                          List<String> ports,
                                                          List<VolumeBinding> volumeBindings,
                                                          Map<String, String> environment, Map<String, Object> runtimeConfig);

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
     * Inspect container to check if it exists
     * @param containerIdOrName container ID or name
     * @return true if container exists, false otherwise
     */
    public abstract boolean inspectContainer(String containerIdOrName);

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
