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
import io.agentscope.runtime.sandbox.manager.collections.RedisContainerMapping;
import io.agentscope.runtime.sandbox.manager.collections.RedisContainerQueue;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.*;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryService;
import io.agentscope.runtime.sandbox.manager.util.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Sandbox Manager for managing container lifecycle
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
    private final int poolSize;
    private final ContainerQueue poolQueue;
    private final SandboxType defaultType;
    private final PortManager portManager;
    private RedisClientWrapper redisClient;
    private RedisContainerMapping redisContainerMapping;
    private final boolean redisEnabled;

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

    public SandboxManager(ManagerConfig managerConfig, String baseUrl, String bearerToken, SandboxType defaultType) {
        // TODO: Support for remote HTTP session needs to be added, currently ignored
        this.managerConfig = managerConfig;
        this.containerManagerType = managerConfig.getClientConfig().getClientType();
        this.storageManager = new StorageManager(managerConfig.getFileSystemConfig());
        this.poolSize = managerConfig.getPoolSize();
        this.defaultType = defaultType;
        this.redisEnabled = managerConfig.getRedisEnabled();
        this.portManager = new PortManager(managerConfig.getPortRange()); // Thread-safe port manager

        logger.info("Initializing SandboxManager with container manager: " + this.containerManagerType);
        logger.info("Container pool size: " + this.poolSize);
        logger.info("Redis enabled: " + this.redisEnabled);

        if (this.redisEnabled) {
            try {
                RedisManagerConfig redisConfig = managerConfig.getRedisConfig();
                this.redisClient = new RedisClientWrapper(redisConfig);

                String pong = this.redisClient.ping();
                logger.info("Redis connection test: " + pong);

                String mappingPrefix = managerConfig.getContainerPrefixKey() + "mapping";
                this.redisContainerMapping = new RedisContainerMapping(this.redisClient, mappingPrefix);

                if (this.poolSize > 0) {
                    String queueName = redisConfig.getRedisContainerPoolKey();
                    this.poolQueue = new RedisContainerQueue(this.redisClient, queueName);
                    logger.info("Using Redis-backed container pool with queue: " + queueName);
                } else {
                    this.poolQueue = new InMemoryContainerQueue();
                }

                logger.info("Redis client initialized successfully for container management");
            } catch (Exception e) {
                logger.severe("Failed to initialize Redis client: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize Redis", e);
            }
        } else {
            this.poolQueue = new InMemoryContainerQueue(); // Use in-memory queue
            logger.info("Using in-memory container storage");
        }

        switch (this.containerManagerType) {
            case DOCKER:
                // TODO: Make DockerClient support DockerClientConfig configuration
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
                this.client = null;
                break;
            case AGENTRUN:
                break;
            case CLOUD:
                break;
            default:
                throw new IllegalArgumentException("Unsupported container manager type: " + this.containerManagerType);
        }

        if (this.poolSize > 0) {
            initContainerPool();
        }
    }

    private void initContainerPool() {
        logger.info("Initializing container pool with size: " + poolSize);

        while (poolQueue.size() < poolSize) {
            try {
                ContainerModel containerModel = createContainer(defaultType, null, null, null);

                if (containerModel != null) {
                    if (poolQueue.size() < poolSize) {
                        poolQueue.enqueue(containerModel);
                        logger.info("Added container to pool: " + containerModel.getContainerName() + " (pool size: " + poolQueue.size() + "/" + poolSize + ")");
                    } else {
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

    public ContainerModel createFromPool(SandboxType sandboxType) {
        if (sandboxType != defaultType) {
            logger.info("Requested type " + sandboxType + " differs from pool type " + defaultType + ", creating directly");
            return createContainer(sandboxType, null, null, null);
        }

        int attempts = 0;
        int maxAttempts = poolSize + 1;

        while (attempts < maxAttempts) {
            attempts++;

            try {
                ContainerModel newContainer = createContainer(defaultType, null, null, null);
                if (newContainer != null) {
                    poolQueue.enqueue(newContainer);
                }

                ContainerModel containerModel = poolQueue.dequeue();

                if (containerModel == null) {
                    logger.warning("No container available in pool after " + attempts + " attempts");
                    continue;
                }

                logger.info("Retrieved container from pool: " + containerModel.getContainerName());

                String currentImage = SandboxRegistryService.getImageByType(defaultType).orElse(containerModel.getVersion());

                if (!currentImage.equals(containerModel.getVersion())) {
                    logger.warning("Container " + containerModel.getContainerName() + " is outdated (has: " + containerModel.getVersion() + ", current: " + currentImage + "), releasing and trying next");
                    releaseContainer(containerModel);
                    continue;
                }

                if (!containerClient.inspectContainer(containerModel.getContainerId())) {
                    logger.warning("Container " + containerModel.getContainerId() + " not found or has been removed externally, trying next");
                    continue;
                }

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

        logger.warning("Failed to get container from pool after " + maxAttempts + " attempts, creating new container");
        return createContainer(sandboxType, null, null, null);
    }

    public ContainerModel createFromPool(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel existingContainer = sandboxMap.get(key);

        if (existingContainer != null) {
            logger.info("Reusing existing container: " + existingContainer.getContainerName() + " (userID: " + userID + ", sessionID: " + sessionID + ")");
            return existingContainer;
        }

        if (redisEnabled && redisContainerMapping != null) {
            existingContainer = redisContainerMapping.get(key);
            if (existingContainer != null) {
                sandboxMap.put(key, existingContainer);
                logger.info("Retrieved container from Redis: " + existingContainer.getContainerName() + " (userID: " + userID + ", sessionID: " + sessionID + ")");
                return existingContainer;
            }
        }

        ContainerModel containerModel = createFromPool(sandboxType);

        if (containerModel == null) {
            logger.severe("Failed to get container from pool");
            return null;
        }

        sandboxMap.put(key, containerModel);

        if (redisEnabled && redisContainerMapping != null) {
            redisContainerMapping.put(key, containerModel);
            logger.info("Stored pool container in Redis");
        }

        logger.info("Added pool container to sandbox map: " + containerModel.getContainerName() + " (userID: " + userID + ", sessionID(key): " + sessionID + ", container sessionId: " + containerModel.getSessionId() + ")");

        return containerModel;
    }

    private ContainerModel createContainer(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment) {
        if (environment == null) {
            environment = new HashMap<>();
        }

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
        Map<String, Integer> portMapping = createPortMapping(ports);
        logger.info("Port mapping: " + portMapping);
        String imageName = SandboxRegistryService.getImageByType(sandboxType).orElse("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest");
        SandboxConfig sandboxConfig = SandboxRegistryService.getConfigByType(sandboxType).orElse(null);
        if (sandboxConfig != null) {
            logger.info("Using registered sandbox configuration: " + sandboxConfig.getDescription());
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
        Map<String, String> readonlyMounts = managerConfig.getFileSystemConfig().getReadonlyMounts();
        if (readonlyMounts != null && !readonlyMounts.isEmpty()) {
            logger.info("Adding readonly mounts: " + readonlyMounts.size() + " mount(s)");
            for (Map.Entry<String, String> entry : readonlyMounts.entrySet()) {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue();
                if (!java.nio.file.Paths.get(hostPath).isAbsolute()) {
                    hostPath = java.nio.file.Paths.get(hostPath).toAbsolutePath().toString();
                    logger.info("Converting relative path to absolute: " + hostPath);
                }
                java.io.File hostFile = new java.io.File(hostPath);
                if (!hostFile.exists()) {
                    logger.warning("Readonly mount host path does not exist: " + hostPath + ", skipping");
                    continue;
                }
                volumeBindings.add(new VolumeBinding(hostPath, containerPath, "ro"));
                logger.info("Added readonly mount: " + hostPath + " -> " + containerPath);
            }
        }
        Map<String, Object> runtimeConfig = Map.of();
        if (sandboxConfig != null) {
            runtimeConfig = sandboxConfig.getRuntimeConfig();
        }
        logger.info("Runtime config: " + runtimeConfig);
        String containerName;
        String prefix = managerConfig.getContainerPrefixKey();
        if (prefix == null || prefix.isEmpty()) {
            prefix = "sandbox";
        }
        if (containerManagerType == ContainerManagerType.DOCKER) {
            containerName = prefix + sessionId.toLowerCase();
        } else {
            containerName = prefix.replace('_', '-') + sessionId.toLowerCase();
        }
        // TODO: Need to check container name uniqueness?
        String containerId = containerClient.createContainer(containerName, imageName, ports, portMapping, volumeBindings, environment, runtimeConfig);
        String[] mappedPorts = portMapping.values().stream().map(String::valueOf).toArray(String[]::new);
        String baseHost;
        String accessPort;
        if (containerManagerType == ContainerManagerType.KUBERNETES) {
            try {
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
            baseHost = "localhost";
            accessPort = mappedPorts[0];
        }
        ContainerModel containerModel = ContainerModel.builder()
                .sessionId(sessionId)
                .containerId(containerId)
                .containerName(containerName)
                .baseUrl(String.format("http://%s:%s/fastapi", baseHost, accessPort))
                .browserUrl(String.format("http://%s:%s/steel-api/%s", baseHost, accessPort, runtimeToken))
                .frontBrowserWS(String.format("ws://%s:%s/steel-api/%s/v1/sessions/cast", baseHost, accessPort, runtimeToken))
                .clientBrowserWS(String.format("ws://%s:%s/steel-api/%s/&sessionId=%s", baseHost, accessPort, runtimeToken, BROWSER_SESSION_ID))
                .artifactsSIO(String.format("http://%s:%s/v1", baseHost, accessPort))
                .ports(mappedPorts)
                .mountDir(mountDir)
                .storagePath(storagePath)
                .runtimeToken(runtimeToken)
                .authToken(runtimeToken)
                .version(imageName)
                .build();
        containerClient.startContainer(containerId);
        return containerModel;
    }

    private void releaseContainer(ContainerModel containerModel) {
        if (containerModel == null) {
            return;
        }
        try {
            logger.info("Releasing container: " + containerModel.getContainerName());
            portManager.releaseContainerPorts(containerModel.getContainerName());
            containerClient.stopContainer(containerModel.getContainerId());
            containerClient.removeContainer(containerModel.getContainerId());
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
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);

        if (redisEnabled && redisContainerMapping != null) {
            ContainerModel existingModel = redisContainerMapping.get(key);
            if (existingModel != null) {
                logger.info("Found existing container in Redis: " + existingModel.getContainerName());
                // Also update local cache
                sandboxMap.put(key, existingModel);
                return existingModel;
            }
        }

        if (sandboxMap.containsKey(key)) {
            return sandboxMap.get(key);
        } else {
            ContainerModel containerModel = createContainer(sandboxType, mountDir, storagePath, environment);
            sandboxMap.put(key, containerModel);

            if (redisEnabled && redisContainerMapping != null) {
                redisContainerMapping.put(key, containerModel);
                logger.info("Stored container in Redis: " + containerModel.getContainerName());
            }

            startSandbox(sandboxType, userID, sessionID);
            return containerModel;
        }
    }

    public void startSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);

        if (containerModel == null && redisEnabled && redisContainerMapping != null) {
            containerModel = redisContainerMapping.get(key);
        }

        if (containerModel != null) {
            containerClient.startContainer(containerModel.getContainerId());
            logger.info("Container status updated to: running");
            sandboxMap.put(key, containerModel);

            if (redisEnabled && redisContainerMapping != null) {
                redisContainerMapping.put(key, containerModel);
            }
        }
    }

    public void stopSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);

        if (containerModel == null && redisEnabled && redisContainerMapping != null) {
            containerModel = redisContainerMapping.get(key);
        }

        if (containerModel != null) {
            containerClient.stopContainer(containerModel.getContainerId());
            logger.info("Container status updated to: stopped");
            sandboxMap.put(key, containerModel);

            if (redisEnabled && redisContainerMapping != null) {
                redisContainerMapping.put(key, containerModel);
            }
        }
    }

    public void removeSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);

        if (containerModel == null && redisEnabled && redisContainerMapping != null) {
            containerModel = redisContainerMapping.get(key);
        }

        if (containerModel != null) {
            containerClient.removeContainer(containerModel.getContainerId());
            sandboxMap.remove(key);

            if (redisEnabled && redisContainerMapping != null) {
                redisContainerMapping.remove(key);
                logger.info("Removed container from Redis");
            }
        }
    }

    public void stopAndRemoveSandbox(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);

        if (containerModel == null && redisEnabled && redisContainerMapping != null) {
            containerModel = redisContainerMapping.get(key);
        }

        if (containerModel != null) {
            try {
                String containerId = containerModel.getContainerId();
                String containerName = containerModel.getContainerName();
                logger.info("Stopping and removing " + sandboxType + " sandbox (Container ID: " + containerId + ", Name: " + containerName + ")");
                portManager.releaseContainerPorts(containerName);
                containerClient.stopContainer(containerId);
                Thread.sleep(1000);
                containerClient.removeContainer(containerId);
                sandboxMap.remove(key);
                if (redisEnabled && redisContainerMapping != null) {
                    redisContainerMapping.remove(key);
                    logger.info("Removed container from Redis");
                }
                logger.info(sandboxType + " sandbox has been successfully removed");
            } catch (Exception e) {
                logger.severe("Error removing " + sandboxType + " sandbox: " + e.getMessage());
                sandboxMap.remove(key);
                if (redisEnabled && redisContainerMapping != null) {
                    redisContainerMapping.remove(key);
                }
            }
        } else {
            logger.warning("Sandbox " + sandboxType + " not found, may have already been removed");
        }
    }

    public String getSandboxStatus(SandboxType sandboxType, String userID, String sessionID) {
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        ContainerModel containerModel = sandboxMap.get(key);

        if (containerModel == null && redisEnabled && redisContainerMapping != null) {
            containerModel = redisContainerMapping.get(key);
            if (containerModel != null) {
                sandboxMap.put(key, containerModel);
            }
        }

        if (containerModel != null) {
            return containerClient.getContainerStatus(containerModel.getContainerId());
        }
        return "not_found";
    }

    public Map<SandboxKey, ContainerModel> getAllSandboxes() {
        Map<SandboxKey, ContainerModel> allSandboxes = new HashMap<>(sandboxMap);

        if (redisEnabled && redisContainerMapping != null) {
            try {
                Map<SandboxKey, ContainerModel> redisData = redisContainerMapping.getAll();
                logger.info("Retrieved " + redisData.size() + " containers from Redis");

                for (Map.Entry<SandboxKey, ContainerModel> entry : redisData.entrySet()) {
                    allSandboxes.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                logger.warning("Failed to retrieve containers from Redis: " + e.getMessage());
            }
        }

        return allSandboxes;
    }

    public ContainerModel getInfo(String identity) {
        if (identity == null || identity.isEmpty()) {
            throw new IllegalArgumentException("Identity cannot be null or empty");
        }

        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getContainerName())) {
                logger.fine("Found container by name: " + identity);
                return model;
            }
        }

        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getSessionId())) {
                logger.fine("Found container by session ID: " + identity);
                return model;
            }
        }

        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getContainerId())) {
                logger.fine("Found container by container ID: " + identity);
                return model;
            }
        }

        for (ContainerModel model : sandboxMap.values()) {
            if (identity.equals(model.getContainerName())) {
                logger.fine("Found container by prefixed session ID: " + identity);
                return model;
            }
        }

        throw new RuntimeException("No container found with identity: " + identity);
    }


    public boolean release(String identity) {
        try {
            ContainerModel containerModel = getInfo(identity);
            logger.info("Releasing container with identity: " + identity);
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

                if (redisEnabled && redisContainerMapping != null) {
                    redisContainerMapping.remove(keyToRemove);
                    logger.info("Removed container from Redis");
                }
            }

            portManager.releaseContainerPorts(containerModel.getContainerName());
            containerClient.stopContainer(containerModel.getContainerId());
            containerClient.removeContainer(containerModel.getContainerId());
            logger.info("Container destroyed: " + containerModel.getContainerName());
            if (containerModel.getMountDir() != null && containerModel.getStoragePath() != null) {
                try {
                    logger.info("Uploading container data to storage: " + containerModel.getStoragePath());
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

    public ContainerManagerType getContainerManagerType() {
        return containerManagerType;
    }

    public ManagerConfig getManagerConfig() {
        return managerConfig;
    }

    public BaseClient getContainerClient() {
        return containerClient;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

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

    @Override
    public void close() {
        logger.info("Closing SandboxManager and cleaning up all resources");

        cleanupAllSandboxes();

        if (storageManager != null) {
            storageManager.close();
        }

        if (portManager != null) {
            portManager.clear();
        }

        if (redisEnabled && redisClient != null) {
            try {
                redisClient.close();
                logger.info("Redis connection closed");
            } catch (Exception e) {
                logger.warning("Error closing Redis connection: " + e.getMessage());
            }
        }

        logger.info("SandboxManager closed successfully");
    }

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

    public String listTools(String identity, String toolType) {
        return "";
    }

    private SandboxClient establishConnection(String sandboxId, String userId, String sessionId) {
        try {
            ContainerModel containerInfo = getInfo(sandboxId, userId, sessionId);
            if (containerInfo.getVersion().contains("sandbox-appworld") || containerInfo.getVersion().contains("sandbox-bfclient")) {
                return new TrainingSandboxClient(containerInfo, 60);
            }
            return new SandboxHttpClient(containerInfo, 60);
        } catch (Exception e) {
            logger.severe("Failed to establish connection to sandbox: " + e.getMessage());
            throw new RuntimeException("Failed to establish connection", e);
        }
    }

    public ContainerModel getInfo(String sandboxId, String userId, String sessionId) {
        for (SandboxType type : SandboxType.values()) {
            SandboxKey key = new SandboxKey(userId, sessionId, type);
            ContainerModel model = sandboxMap.get(key);

            if (model == null && redisEnabled && redisContainerMapping != null) {
                model = redisContainerMapping.get(key);
                if (model != null) {
                    sandboxMap.put(key, model);
                }
            }

            if (model != null) {
                return model;
            }
        }
        return null;
    }

    public Map<String, Object> listTools(String sandboxId, String userId, String sessionId, String toolType) {
        try (SandboxClient client = establishConnection(sandboxId, userId, sessionId)) {
            return client.listTools(toolType, Map.of());
        } catch (Exception e) {
            logger.severe("Error listing tools: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public String callTool(String sandboxId, String userId, String sessionId, String toolName, Map<String, Object> arguments) {
        try (SandboxClient client = establishConnection(sandboxId, userId, sessionId)) {
            return client.callTool(toolName, arguments);
        } catch (Exception e) {
            logger.severe("Error calling tool " + toolName + ": " + e.getMessage());
            e.printStackTrace();
            return "{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"Error calling tool: " + e.getMessage() + "\"}]}";
        }
    }

    public Map<String, Object> addMcpServers(String sandboxId, String userId, String sessionId, Map<String, Object> serverConfigs, boolean overwrite) {
        try (SandboxClient client = establishConnection(sandboxId, userId, sessionId)) {
            return client.addMcpServers(serverConfigs, overwrite);
        } catch (Exception e) {
            logger.severe("Error adding MCP servers: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public boolean releaseSandbox(SandboxType sandboxType, String userId, String sessionId) {
        try {
            stopAndRemoveSandbox(sandboxType, userId, sessionId);
            logger.info("Released sandbox: type=" + sandboxType + ", user=" + userId + ", session=" + sessionId);
            return true;
        } catch (Exception e) {
            logger.severe("Failed to release sandbox: " + e.getMessage());
            return false;
        }
    }
}