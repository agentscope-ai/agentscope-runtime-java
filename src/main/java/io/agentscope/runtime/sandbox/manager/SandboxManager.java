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
package io.agentscope.runtime.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.client.BaseClient;
import io.agentscope.runtime.sandbox.manager.client.DockerClient;
import io.agentscope.runtime.sandbox.manager.client.KubernetesClient;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.collections.ContainerQueue;
import io.agentscope.runtime.sandbox.manager.collections.InMemoryContainerQueue;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.*;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistry;
import io.agentscope.runtime.sandbox.manager.util.PortManager;
import io.agentscope.runtime.sandbox.manager.util.RandomStringGenerator;
import io.agentscope.runtime.sandbox.manager.util.StorageManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Sandbox Manager for managing container lifecycle
 * Implements AutoCloseable for automatic resource cleanup
 * Corresponds to Python's SandboxManager class
 */
public class SandboxManager implements AutoCloseable {
    private final Map<SandboxKey, ContainerModel> sandboxMap = new HashMap<>();
    private final ContainerManagerType containerManagerType;
    public com.github.dockerjava.api.DockerClient client;
    Logger logger = Logger.getLogger(SandboxManager.class.getName());
    String BROWSER_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private final ManagerConfig managerConfig;
    private BaseClient containerClient;
    private final StorageManager storageManager;

    // Container pool management
    private final int poolSize;
    private final ContainerQueue poolQueue;
    private final SandboxType defaultType;

    // Port management (thread-safe)
    private final PortManager portManager;

    public SandboxManager() {
        this(null, null);
    }

    public SandboxManager(String baseUrl, String bearerToken) {
        this(new ManagerConfig.Builder().build(), baseUrl, bearerToken);
    }

    public SandboxManager(ManagerConfig managerConfig) {
        this(managerConfig, null, null);
    }

    public SandboxManager(SandboxType defaultType) {
        this(new ManagerConfig.Builder().build(), null, null, defaultType);
    }

    public SandboxManager(ManagerConfig managerConfig, String baseUrl, String bearerToken) {
        this(managerConfig, baseUrl, bearerToken, SandboxType.BASE);
    }

    /**
     * Constructor supporting Kubernetes configuration
     *
     * @param managerConfig manager configuration
     */
    public SandboxManager(ManagerConfig managerConfig, String baseUrl, String bearerToken, SandboxType defaultType) {
        // Todo: 这里需要支持远程的http session，目前先忽略
        this.managerConfig = managerConfig;
        this.containerManagerType = managerConfig.getClientConfig().getClientType();
        this.storageManager = new StorageManager(managerConfig.getFileSystemConfig());
        this.poolSize = managerConfig.getPoolSize();
        this.defaultType = defaultType;
        this.poolQueue = new InMemoryContainerQueue(); // Use in-memory queue for now
        this.portManager = new PortManager(managerConfig.getPortRange()); // Thread-safe port manager

        logger.info("Initializing SandboxManager with container manager: " + this.containerManagerType);
        logger.info("Container pool size: " + this.poolSize);

        switch (this.containerManagerType) {
            case DOCKER:
                // Todo: Make DockerClient support DockerClientConfig configuration
                DockerClient dockerClient = new DockerClient();
                this.containerClient = dockerClient;
                this.client = dockerClient.connectDocker();
                break;
            case KUBERNETES:
                KubernetesClient kubernetesClient;
                if (managerConfig.getClientConfig() instanceof KubernetesClientConfig kubernetesClientConfig) {
                    kubernetesClient = new KubernetesClient(kubernetesClientConfig);
                } else {
                    logger.warning("Provided clientConfig is not an instance of KubernetesClientConfig, using default configuration");
                    kubernetesClient = new KubernetesClient();
                }
                this.containerClient = kubernetesClient;
                kubernetesClient.connect();
                this.client = null; // Kubernetes does not need Docker client
                break;
            case AGENTRUN:
                break;
            case CLOUD:
                break;
            default:
                throw new IllegalArgumentException("Unsupported container manager type: " + this.containerManagerType);
        }

        // Initialize container pool if pool size > 0
        if (this.poolSize > 0) {
            initContainerPool();
        }
    }

    /**
     * Initialize container pool
     * Corresponds to Python's _init_container_pool()
     */
    private void initContainerPool() {
        logger.info("Initializing container pool with size: " + poolSize);

        while (poolQueue.size() < poolSize) {
            try {
                // Create a container for the pool using default pool type
                ContainerModel containerModel = createContainer(defaultType, null, null, null);

                if (containerModel != null) {
                    // Check pool size again to avoid race condition
                    if (poolQueue.size() < poolSize) {
                        poolQueue.enqueue(containerModel);
                        logger.info("Added container to pool: " + containerModel.getContainerName() + " (pool size: " + poolQueue.size() + "/" + poolSize + ")");
                    } else {
                        // Pool size reached, release this container
                        logger.info("Pool size limit reached, releasing container: " + containerModel.getContainerName());
                        releaseContainer(containerModel);
                        break;
                    }
                } else {
                    logger.severe("Failed to create container for pool");
                    break;
                }
            } catch (Exception e) {
                logger.severe("Error initializing container pool: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        logger.info("Container pool initialization complete. Pool size: " + poolQueue.size());
    }

    /**
     * Create a container from the pool
     * Corresponds to Python's create_from_pool()
     *
     * @param sandboxType The type of sandbox to create
     * @return The container name, or null if failed
     */
    public ContainerModel createFromPool(SandboxType sandboxType) {
        // If requested type is not the default pool type, create directly
        if (sandboxType != defaultType) {
            logger.info("Requested type " + sandboxType + " differs from pool type " + defaultType + ", creating directly");
            return createContainer(sandboxType, null, null, null);
        }

        int attempts = 0;
        int maxAttempts = poolSize + 1;

        while (attempts < maxAttempts) {
            attempts++;

            try {
                // Create a new container to add to the pool first
                ContainerModel newContainer = createContainer(defaultType, null, null, null);
                if (newContainer != null) {
                    poolQueue.enqueue(newContainer);
                }

                // Dequeue a container from the pool
                ContainerModel containerModel = poolQueue.dequeue();

                if (containerModel == null) {
                    logger.warning("No container available in pool after " + attempts + " attempts");
                    continue;
                }

                logger.info("Retrieved container from pool: " + containerModel.getContainerName());

                // Verify the container version matches the current registered image
                String currentImage = SandboxRegistry.getImageByType(defaultType).orElse(containerModel.getVersion());

                if (!currentImage.equals(containerModel.getVersion())) {
                    logger.warning("Container " + containerModel.getContainerName() + " is outdated (has: " + containerModel.getVersion() + ", current: " + currentImage + "), releasing and trying next");
                    releaseContainer(containerModel);
                    continue;
                }

                // Verify container still exists
                if (!containerClient.inspectContainer(containerModel.getContainerId())) {
                    logger.warning("Container " + containerModel.getContainerId() + " not found or has been removed externally, trying next");
                    // Container doesn't exist, just continue to next one
                    continue;
                }

                // Verify container is still running
                String status = containerClient.getContainerStatus(containerModel.getContainerId());
                if (!"running".equals(status)) {
                    logger.warning("Container " + containerModel.getContainerId() + " is not running (status: " + status + "), trying next");
                    releaseContainer(containerModel);
                    continue;
                }

                logger.info("Successfully retrieved running container from pool: " + containerModel.getContainerName());
                return containerModel;

            } catch (Exception e) {
                logger.severe("Error getting container from pool (attempt " + attempts + "): " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Failed to get from pool, create a new one
        logger.warning("Failed to get container from pool after " + maxAttempts + " attempts, creating new container");
        return createContainer(sandboxType, null, null, null);
    }

    /**
     * Create a container from the pool and add it to the sandbox map for management
     * This allows the container to be released using release(sessionId)
     *
     * @param sandboxType The type of sandbox to create
     * @param userID      The user ID
     * @param sessionID   The session ID
     * @return The container model, or null if failed
     */
    public ContainerModel createFromPool(SandboxType sandboxType, String userID, String sessionID) {
        // Get container from pool (call the single-parameter version)
        ContainerModel containerModel = createFromPool(sandboxType);

        if (containerModel == null) {
            logger.severe("Failed to get container from pool");
            return null;
        }

        // Add to sandbox map for unified management using the provided sessionID as the key
        // but keep the container's original random sessionId
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        sandboxMap.put(key, containerModel);

        logger.info("Added pool container to sandbox map: " + containerModel.getContainerName() + " (userID: " + userID + ", sessionID(key): " + sessionID + ", container sessionId: " + containerModel.getSessionId() + ")");

        return containerModel;
    }

    /**
     * Helper method to create a container (internal use)
     * This is separated from getSandbox for pool management
     */
    private ContainerModel createContainer(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment) {
        // Initialize environment if null
        if (environment == null) {
            environment = new HashMap<>();
        }

        // Validate environment values
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                logger.warning("Environment variable " + entry.getKey() + " has null value");
                return null;
            }
        }

        String workdir = "/workspace";
        String default_mount_dir = managerConfig.getFileSystemConfig().getMountDir();
        String[] portsArray = {"80/tcp"};
        List<String> ports = Arrays.asList(portsArray);

        // Create port mapping
        Map<String, Integer> portMapping = createPortMapping(ports);
        logger.info("Port mapping: " + portMapping);

        // Get image name and configuration from registry
        String imageName = SandboxRegistry.getImageByType(sandboxType).orElse("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest");

        // Get sandbox configuration if available
        SandboxConfig sandboxConfig = SandboxRegistry.getConfigByType(sandboxType).orElse(null);
        if (sandboxConfig != null) {
            logger.info("Using registered sandbox configuration: " + sandboxConfig.getDescription());
            // Merge environment variables from config
            if (sandboxConfig.getEnvironment() != null && !sandboxConfig.getEnvironment().isEmpty()) {
                Map<String, String> mergedEnv = new HashMap<>(sandboxConfig.getEnvironment());
                mergedEnv.putAll(environment);
                environment = mergedEnv;
            }
        }

        logger.info("Checking image: " + imageName);
        if (containerManagerType == ContainerManagerType.DOCKER) {
            if (!containerClient.ensureImageAvailable(imageName)) {
                logger.severe("Can not get image: " + imageName);
                throw new RuntimeException("Pull image failed: " + imageName);
            }
            logger.info("Docker image is ready: " + imageName);
        } else if (containerManagerType == ContainerManagerType.KUBERNETES) {
            logger.info("Kubernetes image is ready: " + imageName);
        }

        String sessionId = RandomStringGenerator.generateRandomString(22);
        String currentDir = System.getProperty("user.dir");

        if (mountDir == null || mountDir.isEmpty()) {
            mountDir = currentDir + "/" + default_mount_dir + "/" + sessionId;
        }

        if (this.managerConfig.getClientConfig().getClientType() == ContainerManagerType.AGENTRUN) {
            mountDir = Paths.get(mountDir).toAbsolutePath().toString();
        }

        java.io.File file = new java.io.File(mountDir);
        if (!file.exists()) {
            file.mkdirs();
        }

        // Todo：Currently using global storage path if not provided, still need to wait for next movement of python version
        if (storagePath == null) {
            storagePath = managerConfig.getFileSystemConfig().getStorageFolderPath();
        }

        // If storage path is configured and not AgentRun deployment, download files from storage to mount directory
        if (!mountDir.isEmpty() && !storagePath.isEmpty() && containerManagerType != ContainerManagerType.AGENTRUN) {
            logger.info("Downloading from storage path: " + storagePath + " to mount dir: " + mountDir);
            boolean downloadSuccess = storageManager.downloadFolder(storagePath, mountDir);
            if (downloadSuccess) {
                logger.info("Successfully downloaded files from storage");
            } else {
                logger.warning("Failed to download files from storage, continuing with empty mount dir");
            }
        }

        String runtimeToken = RandomStringGenerator.generateRandomString(32);
        environment.put("SECRET_TOKEN", runtimeToken);

        List<VolumeBinding> volumeBindings = new ArrayList<>();
        volumeBindings.add(new VolumeBinding(mountDir, workdir, "rw"));

        // Add read-only mounts
        Map<String, String> readonlyMounts = managerConfig.getFileSystemConfig().getReadonlyMounts();
        if (readonlyMounts != null && !readonlyMounts.isEmpty()) {
            logger.info("Adding readonly mounts: " + readonlyMounts.size() + " mount(s)");
            for (Map.Entry<String, String> entry : readonlyMounts.entrySet()) {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue();

                // Ensure host path is absolute path
                if (!java.nio.file.Paths.get(hostPath).isAbsolute()) {
                    hostPath = java.nio.file.Paths.get(hostPath).toAbsolutePath().toString();
                    logger.info("Converting relative path to absolute: " + hostPath);
                }

                // Check if host path exists
                java.io.File hostFile = new java.io.File(hostPath);
                if (!hostFile.exists()) {
                    logger.warning("Readonly mount host path does not exist: " + hostPath + ", skipping");
                    continue;
                }

                volumeBindings.add(new VolumeBinding(hostPath, containerPath, "ro"));
                logger.info("Added readonly mount: " + hostPath + " -> " + containerPath);
            }
        }

        String runtimeConfig = "runc"; // or "nvidia" etc.
        String containerName = this.managerConfig.getContainerPrefixKey() + sessionId.toLowerCase();

        // Todo: need to judge container name uniqueness?

        // Use unified BaseClient interface to create container
        String containerId = containerClient.createContainer(containerName, imageName, ports, portMapping, volumeBindings, environment, runtimeConfig);

        // Convert mapped host ports to string array
        String[] mappedPorts = portMapping.values().stream().map(String::valueOf).toArray(String[]::new);

        // Determine correct access URL based on container manager type
        String baseHost;
        String accessPort;

        if (containerManagerType == ContainerManagerType.KUBERNETES) {
            // In Kubernetes environment, use LoadBalancer's External IP
            try {
                // Wait for LoadBalancer to assign External IP (max 60 seconds)
                String externalIP = ((KubernetesClient) containerClient).waitForLoadBalancerExternalIP(containerName, 60);
                if (externalIP != null && !externalIP.isEmpty()) {
                    baseHost = externalIP;
                    accessPort = "80"; // LoadBalancer uses port 80 by default
                    logger.info("Kubernetes LoadBalancer environment: using External IP " + externalIP + " port 80");
                } else {
                    logger.warning("Unable to get LoadBalancer External IP, using localhost and mapped port");
                    baseHost = "localhost";
                    accessPort = mappedPorts[0];
                }
            } catch (Exception e) {
                logger.warning("Failed to get LoadBalancer External IP, using localhost and mapped port: " + e.getMessage());
                baseHost = "localhost";
                accessPort = mappedPorts[0];
            }
        } else {
            // Docker environment uses mapped ports
            baseHost = "localhost";
            accessPort = mappedPorts[0];
        }

        ContainerModel containerModel = ContainerModel.builder().sessionId(sessionId).containerId(containerId).containerName(containerName).baseUrl(String.format("http://%s:%s/fastapi", baseHost, accessPort)).browserUrl(String.format("http://%s:%s/steel-api/%s", baseHost, accessPort, runtimeToken)).frontBrowserWS(String.format("ws://%s:%s/steel-api/%s/v1/sessions/cast", baseHost, accessPort, runtimeToken)).clientBrowserWS(String.format("ws://%s:%s/steel-api/%s/&sessionId=%s", baseHost, accessPort, runtimeToken, BROWSER_SESSION_ID)).artifactsSIO(String.format("http://%s:%s/v1", baseHost, accessPort)).ports(mappedPorts).mountDir(mountDir).storagePath(storagePath).runtimeToken(runtimeToken).authToken(runtimeToken).version(imageName).build();

        containerClient.startContainer(containerId);

        return containerModel;
    }

    /**
     * Release a container (stop and remove)
     */
    private void releaseContainer(ContainerModel containerModel) {
        if (containerModel == null) {
            return;
        }

        try {
            logger.info("Releasing container: " + containerModel.getContainerName());

            // Release ports first
            portManager.releaseContainerPorts(containerModel.getContainerName());

            // Stop and remove container
            containerClient.stopContainer(containerModel.getContainerId());
            containerClient.removeContainer(containerModel.getContainerId());

            // Upload to storage if configured
            if (containerModel.getMountDir() != null && containerModel.getStoragePath() != null) {
                storageManager.uploadFolder(containerModel.getMountDir(), containerModel.getStoragePath());
            }
        } catch (Exception e) {
            logger.warning("Error releasing container " + containerModel.getContainerName() + ": " + e.getMessage());
        }
    }

    private static boolean canBindPort(int port) {
        List<String> addressesToTest = Arrays.asList("0.0.0.0", "127.0.0.1", "localhost");

        for (String addr : addressesToTest) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(false);
                socket.bind(new InetSocketAddress(addr, port), 1);
                return true;
            } catch (IOException e) {
                // Port is in use or cannot bind to this address
            }
        }
        return false;
    }

    public ContainerModel getSandbox(SandboxType sandboxType, String userID, String sessionID) {
        return getSandbox(sandboxType, null, null, null, userID, sessionID);
    }

    public ContainerModel getSandbox(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment, String userID, String sessionID) {
        // Use composite key userID + sessionID + sandboxType to uniquely identify container
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        if (sandboxMap.containsKey(key)) {
            return sandboxMap.get(key);
        } else {
            ContainerModel containerModel = createContainer(sandboxType, mountDir, storagePath, environment);
            sandboxMap.put(key, containerModel);
            startSandbox(sandboxType, userID, sessionID);
            return containerModel;
        }
    }

    /**
     * Start sandbox container
     *
     * @param sandboxType sandbox type
     * @param userID      user ID
     * @param sessionID   session ID
     */
    public void startSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);
        if (containerModel != null) {
            containerClient.startContainer(containerModel.getContainerId());
            logger.info("Container status updated to: running");
            sandboxMap.put(key, containerModel);
        }
    }

    /**
     * Stop sandbox container
     *
     * @param sandboxType sandbox type
     * @param userID      user ID
     * @param sessionID   session ID
     */
    public void stopSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);
        if (containerModel != null) {
            containerClient.stopContainer(containerModel.getContainerId());
            logger.info("Container status updated to: stopped");
            sandboxMap.put(key, containerModel);
        }
    }

    /**
     * Remove sandbox container
     *
     * @param sandboxType sandbox type
     * @param userID      user ID
     * @param sessionID   session ID
     */
    public void removeSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);
        if (containerModel != null) {
            containerClient.removeContainer(containerModel.getContainerId());
            sandboxMap.remove(key);
        }
    }

    public void stopAndRemoveSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);
        if (containerModel != null) {
            try {
                String containerId = containerModel.getContainerId();
                String containerName = containerModel.getContainerName();

                logger.info("Stopping and removing " + sandboxType + " sandbox (Container ID: " + containerId + ", Name: " + containerName + ")");

                // Release ports
                portManager.releaseContainerPorts(containerName);

                containerClient.stopContainer(containerId);

                Thread.sleep(1000);

                // Force remove the container
                containerClient.removeContainer(containerId);

                // Remove from mapping
                sandboxMap.remove(key);

                logger.info(sandboxType + " sandbox has been successfully removed");
            } catch (Exception e) {
                logger.severe("Error removing " + sandboxType + " sandbox: " + e.getMessage());
                sandboxMap.remove(key);
            }
        } else {
            logger.warning("Sandbox " + sandboxType + " not found, may have already been removed");
        }
    }

    /**
     * Get sandbox status
     *
     * @param sandboxType sandbox type
     * @return sandbox status
     */
    public String getSandboxStatus(SandboxType sandboxType, String userID, String sessionID) {
        // Use composite key to find container status
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);
        if (containerModel != null) {
            return containerClient.getContainerStatus(containerModel.getContainerId());
        }
        return "not_found";
    }

    /**
     * Get all sandbox information
     *
     * @return sandbox mapping
     */
    public Map<SandboxKey, ContainerModel> getAllSandboxes() {
        return new HashMap<>(sandboxMap);
    }

    /**
     * Get container information by identity
     * Corresponds to Python's get_info(identity) method
     *
     * @param identity Container identifier - can be:
     *                 - containerName (e.g., "runtime_sandbox_container_abc123")
     *                 - sessionId (e.g., "abc123")
     *                 - containerId (Docker container ID)
     * @return ContainerModel if found
     * @throws RuntimeException if no container found with the given identity
     */
    public ContainerModel getInfo(String identity) {
        if (identity == null || identity.isEmpty()) {
            throw new IllegalArgumentException("Identity cannot be null or empty");
        }

        // 1. Try to find by container name directly
        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getContainerName())) {
                logger.fine("Found container by name: " + identity);
                return model;
            }
        }

        // 2. Try to find by session ID
        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getSessionId())) {
                logger.fine("Found container by session ID: " + identity);
                return model;
            }
        }

        // 3. Try to find by container ID
        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getContainerId())) {
                logger.fine("Found container by container ID: " + identity);
                return model;
            }
        }

        // 4. Try as prefixed session ID (container_prefix + sessionId)
        String prefixedName = managerConfig.getContainerPrefixKey() + identity.toLowerCase();
        for (ContainerModel model : sandboxMap.values()) {
            if (prefixedName.equals(model.getContainerName())) {
                logger.fine("Found container by prefixed session ID: " + identity);
                return model;
            }
        }

        throw new RuntimeException("No container found with identity: " + identity);
    }

    /**
     * Release (destroy) a container by identity
     * Corresponds to Python's release(identity) method
     *
     * @param identity Container identifier (containerName, sessionId, or containerId)
     * @return true if successfully released, false otherwise
     */
    public boolean release(String identity) {
        try {
            // Get container info
            ContainerModel containerModel = getInfo(identity);

            logger.info("Releasing container with identity: " + identity + " (container: " + containerModel.getContainerName() + ")");

            // Remove from sandboxMap first
            SandboxKey keyToRemove = null;
            for (Map.Entry<SandboxKey, ContainerModel> entry : sandboxMap.entrySet()) {
                if (entry.getValue().getContainerName().equals(containerModel.getContainerName())) {
                    keyToRemove = entry.getKey();
                    break;
                }
            }

            if (keyToRemove != null) {
                sandboxMap.remove(keyToRemove);
                logger.info("Removed container from sandbox map: " + keyToRemove);
            }

            // Release ports
            portManager.releaseContainerPorts(containerModel.getContainerName());

            // Stop and remove container
            containerClient.stopContainer(containerModel.getContainerId());
            containerClient.removeContainer(containerModel.getContainerId());

            logger.info("Container destroyed: " + containerModel.getContainerName());

            // Upload to storage if configured
            if (containerModel.getMountDir() != null && containerModel.getStoragePath() != null) {
                try {
                    logger.info("Uploading container data to storage: " + containerModel.getStoragePath());
                    System.out.println("Uploading from " + containerModel.getMountDir() + " to " + containerModel.getStoragePath());
                    boolean uploaded = storageManager.uploadFolder(containerModel.getMountDir(), containerModel.getStoragePath());
                    if (uploaded) {
                        logger.info("Successfully uploaded container data to storage");
                    } else {
                        logger.warning("Failed to upload container data to storage");
                    }
                } catch (Exception e) {
                    logger.warning("Failed to upload to storage: " + e.getMessage());
                }
            }

            return true;

        } catch (RuntimeException e) {
            logger.warning("Container not found for identity: " + identity);
            return false;
        } catch (Exception e) {
            logger.severe("Failed to release container: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get container manager type
     *
     * @return container manager type
     */
    public ContainerManagerType getContainerManagerType() {
        return containerManagerType;
    }

    /**
     * Get manager configuration
     *
     * @return manager configuration
     */
    public ManagerConfig getManagerConfig() {
        return managerConfig;
    }

    /**
     * Get container client
     *
     * @return container client
     */
    public BaseClient getContainerClient() {
        return containerClient;
    }

    /**
     * Get storage manager
     *
     * @return storage manager
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }

    /**
     * Clean up all sandbox containers including the pool
     * Corresponds to Python's cleanup() method
     */
    public void cleanupAllSandboxes() {
        logger.info("Starting cleanup of all sandbox containers...");

        // Clean up pool first (corresponds to Python's pool cleanup)
        try {
            logger.info("Cleaning up container pool (current size: " + poolQueue.size() + ")");
            while (!poolQueue.isEmpty()) {
                ContainerModel containerModel = poolQueue.dequeue();
                if (containerModel != null) {
                    logger.info("Destroying pool container: " + containerModel.getContainerId());
                    releaseContainer(containerModel);
                }
            }
            logger.info("Container pool cleanup complete");
        } catch (Exception e) {
            logger.severe("Error cleaning up container pool: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up rest of containers in sandboxMap
        if (sandboxMap.isEmpty()) {
            logger.info("No additional sandbox containers to clean up");
        } else {
            logger.info("Cleaning up " + sandboxMap.size() + " active sandbox containers");
            logger.info("Sandbox types: " + sandboxMap.keySet());

            for (SandboxKey key : new HashSet<>(sandboxMap.keySet())) {
                try {
                    logger.info("Cleaning up " + key.getSandboxType() + " sandbox for user " + key.getUserID() + " session " + key.getSessionID());
                    stopAndRemoveSandbox(key.getSandboxType(), key.getUserID(), key.getSessionID());
                    logger.info(key.getSandboxType() + " sandbox cleanup complete");
                } catch (Exception e) {
                    logger.severe("Error cleaning up " + key.getSandboxType() + " sandbox: " + e.getMessage());
                }
            }

            sandboxMap.clear();
        }

        logger.info("All sandbox containers have been cleaned up!");
    }

    /**
     * Close resources including storage manager and port manager
     * Implements AutoCloseable for try-with-resources support
     * Corresponds to Python's __exit__() method
     */
    @Override
    public void close() {
        logger.info("Closing SandboxManager and cleaning up all resources");

        // Cleanup all containers first
        cleanupAllSandboxes();

        // Close storage manager
        if (storageManager != null) {
            storageManager.close();
        }

        // Clear port manager
        if (portManager != null) {
            portManager.clear();
        }

        logger.info("SandboxManager closed successfully");
    }

    /**
     * Find available ports
     *
     * @param count number of ports to find
     * @return list of available ports
     */
    private List<Integer> findFreePorts(int count) {
        List<Integer> freePorts = new ArrayList<>();
        int startPort = managerConfig.getPortRange().getStart();
        int endPort = managerConfig.getPortRange().getEnd();
        for (int i = 0; i < count && freePorts.size() < count; i++) {
            for (int port = startPort + i; port < endPort; port++) {
                if (isPortAvailable(port)) {
                    freePorts.add(port);
                    break;
                }
            }
        }
        return freePorts;
    }

    /**
     * Check if a port is available
     *
     * @param port port number
     * @return whether the port is available
     */
    public boolean isPortAvailable(int port) {
        boolean canConnect = testConnectionWithRetries("localhost", port, 2);
        if (canConnect) {
            return false;
        }
        return canBindPort(port);
    }

    private boolean testConnectionWithRetries(String host, int port, int retries) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < retries; i++) {
                futures.add(executor.submit(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(host, port), 2000);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                }));
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(2000L * retries, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }

            for (Future<Boolean> future : futures) {
                try {
                    if (future.get()) {
                        return true;
                    }
                } catch (Exception e) {
                    // ignore exceptions from individual tasks
                }
            }
            return false;
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Create port mapping using thread-safe port manager
     *
     * @param containerPorts list of container ports
     * @return port mapping Map
     */
    private Map<String, Integer> createPortMapping(List<String> containerPorts) {
        Map<String, Integer> portMapping = new HashMap<>();

        if (containerPorts != null && !containerPorts.isEmpty()) {
            if (containerManagerType == ContainerManagerType.KUBERNETES) {
                // For Kubernetes LoadBalancer, use port 80
                for (String containerPort : containerPorts) {
                    portMapping.put(containerPort, 80);
                }
            } else {
                // For Docker, use PortManager for thread-safe port allocation
                int[] allocatedPorts = portManager.allocatePorts(containerPorts.size());
                if (allocatedPorts == null) {
                    logger.severe("Failed to allocate " + containerPorts.size() + " ports");
                    return portMapping;
                }

                for (int i = 0; i < containerPorts.size() && i < allocatedPorts.length; i++) {
                    String containerPort = containerPorts.get(i);
                    Integer hostPort = allocatedPorts[i];
                    portMapping.put(containerPort, hostPort);
                    logger.fine("Mapped container port " + containerPort + " to host port " + hostPort);
                }
            }
        }

        return portMapping;
    }

    public String listTools(String identity, String toolType){
        return "";
    }
}