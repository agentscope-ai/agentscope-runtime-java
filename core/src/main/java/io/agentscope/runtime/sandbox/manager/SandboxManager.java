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

import io.agentscope.runtime.sandbox.manager.client.*;
import io.agentscope.runtime.sandbox.manager.client.config.AgentRunClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.FcClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.collections.ContainerQueue;
import io.agentscope.runtime.sandbox.manager.collections.InMemoryContainerQueue;
import io.agentscope.runtime.sandbox.manager.collections.RedisContainerMapping;
import io.agentscope.runtime.sandbox.manager.collections.RedisContainerQueue;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.*;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryService;
import io.agentscope.runtime.sandbox.manager.remote.RemoteHttpClient;
import io.agentscope.runtime.sandbox.manager.remote.RemoteWrapper;
import io.agentscope.runtime.sandbox.manager.remote.RequestMethod;
import io.agentscope.runtime.sandbox.manager.util.*;

import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Sandbox Manager for managing container lifecycle
 */
public class SandboxManager implements AutoCloseable {
    private final Map<SandboxKey, ContainerModel> sandboxMap = new HashMap<>();
    private final ContainerManagerType containerManagerType;
    Logger logger = Logger.getLogger(SandboxManager.class.getName());
    String BROWSER_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private final ManagerConfig managerConfig;
    private BaseClient containerClient;
    private final StorageManager storageManager;
    private final int poolSize;
    private ContainerQueue poolQueue;
    private final PortManager portManager;
    private RedisClientWrapper redisClient;
    private RedisContainerMapping redisContainerMapping;
    private final boolean redisEnabled;
    private final RemoteHttpClient remoteHttpClient;
    private AgentBayClient agentBayClient;

    public AgentBayClient getAgentBayClient() {
        return agentBayClient;
    }


    public SandboxManager() {
        this(null, null);
    }

    public SandboxManager(String baseUrl, String bearerToken) {
        this(new ManagerConfig.Builder().baseUrl(baseUrl).bearerToken(bearerToken).build());
    }

    public SandboxManager(ManagerConfig managerConfig) {
        this(managerConfig, managerConfig.getBaseUrl(), managerConfig.getBearerToken());
    }

    public SandboxManager(ManagerConfig managerConfig, String baseUrl, String bearerToken) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            this.remoteHttpClient = new RemoteHttpClient(baseUrl, bearerToken);
            logger.info("Initialized SandboxManager in remote mode with base URL: " + baseUrl);
            this.managerConfig = managerConfig != null ? managerConfig : new ManagerConfig.Builder().build();
            this.containerManagerType = ContainerManagerType.CLOUD;
            this.storageManager = null;
            this.poolSize = 0;
            this.redisEnabled = false;
            this.portManager = null;
            this.poolQueue = null;
            this.containerClient = null;
            return;
        }

        this.remoteHttpClient = null;
        this.poolQueue = null;
        this.managerConfig = managerConfig;
        this.containerManagerType = managerConfig.getClientConfig().getClientType();
        this.storageManager = new StorageManager(managerConfig.getFileSystemConfig());
        this.poolSize = managerConfig.getPoolSize();
        this.redisEnabled = managerConfig.getRedisEnabled();
        this.portManager = new PortManager(managerConfig.getPortRange());

        logger.info("Initializing SandboxManager with container manager: " + this.containerManagerType);
        logger.info("Container pool size: " + this.poolSize);
        logger.info("Redis enabled: " + this.redisEnabled);
    }

    public void start() {
        if (this.redisEnabled) {
            try {
                RedisManagerConfig redisConfig = managerConfig.getRedisConfig();
                this.redisClient = new RedisClientWrapper(redisConfig);

                String pong = this.redisClient.ping();
                logger.info("Redis connection test: " + pong);

                String mappingPrefix = managerConfig.getContainerPrefixKey() + "mapping";
                this.redisContainerMapping = new RedisContainerMapping(this.redisClient, mappingPrefix);

                String queueName = redisConfig.getRedisContainerPoolKey();
                this.poolQueue = new RedisContainerQueue(this.redisClient, queueName);
                logger.info("Using Redis-backed container pool with queue: " + queueName);

                logger.info("Redis client initialized successfully for container management");
            } catch (Exception e) {
                logger.severe("Failed to initialize Redis client: " + e.getMessage());
                throw new RuntimeException("Failed to initialize Redis", e);
            }
        } else {
            this.poolQueue = new InMemoryContainerQueue(); // Use in-memory queue
            logger.info("Using in-memory container storage");
        }

        logger.info("Using container type: " + this.containerManagerType);

        if (managerConfig.getAgentBayApiKey() != null) {
            agentBayClient = new AgentBayClient(managerConfig.getAgentBayApiKey());
        }

        switch (this.containerManagerType) {
            case DOCKER:
                DockerClientConfig dockerClientConfig;
                if (managerConfig.getClientConfig() instanceof DockerClientConfig existingConfig) {
                    dockerClientConfig = existingConfig;
                } else {
                    dockerClientConfig = DockerClientConfig.builder().build();
                }
                DockerClient dockerClient = new DockerClient(dockerClientConfig, portManager);
                logger.info("Docker client created: " + dockerClient);

                this.containerClient = dockerClient;
                dockerClient.connectDocker();
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
                break;
            case AGENTRUN:
                this.containerClient = getAgentRunClient(managerConfig);
                containerClient.connect();
                break;
            case FC:
                this.containerClient = getFcClient(managerConfig);
                containerClient.connect();
            case CLOUD:
                break;
            default:
                throw new IllegalArgumentException("Unsupported container manager type: " + this.containerManagerType);
        }

        if (this.poolSize > 0) {
            initContainerPool();
        }
    }

    private FcClient getFcClient(ManagerConfig managerConfig) {
        FcClient fcClient;
        try {
            if (managerConfig.getClientConfig() instanceof FcClientConfig fcClientConfig) {
                fcClient = new FcClient(fcClientConfig);
            } else {
                throw new RuntimeException("Provided clientConfig is not an instance of FC config, using default configuration");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FC client");
        }
        return fcClient;
    }

    private AgentRunClient getAgentRunClient(ManagerConfig managerConfig) {
        AgentRunClient agentRunClient;
        try {
            if (managerConfig.getClientConfig() instanceof AgentRunClientConfig agentRunClientConfig) {
                agentRunClient = new AgentRunClient(agentRunClientConfig);
            } else {
                throw new RuntimeException("Provided clientConfig is not an instance of AgentRun config, using default configuration");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize agentrun client");
        }
        return agentRunClient;
    }

    private void initContainerPool() {
        logger.info("Initializing container pool with size: " + poolSize);

        while (poolQueue.size() < poolSize) {
            for (SandboxType type : managerConfig.getDefaultSandboxType()) {
                if (type == SandboxType.AGENTBAY) {
                    logger.warning("Skipping AgentBay sandbox type for container pool initialization");
                    continue;
                }
                try {
                    ContainerModel containerModel = createContainer(type);

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
        }
        logger.info("Container pool initialization complete. Pool size: " + poolQueue.size());
    }

    public ContainerModel createFromPool(SandboxType sandboxType, String imageId, Map<String, String> labels) {
        int attempts = 0;
        int maxAttempts = poolSize + 1;

        while (attempts < maxAttempts) {
            attempts++;

            try {
                ContainerModel newContainer = createContainer(sandboxType, null, null, null, imageId, labels);
                if (newContainer != null) {
                    poolQueue.enqueue(newContainer);
                }

                ContainerModel containerModel = poolQueue.dequeue();

                if (containerModel == null) {
                    logger.warning("No container available in pool after " + attempts + " attempts");
                    continue;
                }

                logger.info("Retrieved container from pool: " + containerModel.getContainerName());

                String currentImage = SandboxRegistryService.getImageByType(sandboxType).orElse(containerModel.getVersion());

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
        return createContainer(sandboxType, null, null, null, imageId, labels);
    }

    public ContainerModel createFromPool(SandboxType sandboxType, String userID, String sessionID) {
        return createFromPool(sandboxType, userID, sessionID, null, null);
    }

    @RemoteWrapper
    public ContainerModel createFromPool(SandboxType sandboxType, String userID, String sessionID, String imageId, Map<String, String> labels) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding createFromPool(with userID/sessionID) to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxType", sandboxType != null ? sandboxType.name() : null);
            requestData.put("userID", userID);
            requestData.put("sessionID", sessionID);
            requestData.put("imageId", imageId);
            requestData.put("labels", labels);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/createFromPool",
                    requestData,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                ContainerModel containerModel = ContainerModel.fromMap(resultMap);
                poolQueue.enqueue(containerModel);
                return containerModel;
            }
            return null;
        }

        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType, imageId);
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

        ContainerModel containerModel = createFromPool(sandboxType, imageId, labels);

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

    public ContainerModel createContainer(SandboxType sandboxType) {
        return createContainer(sandboxType, null, null, null);
    }

    public ContainerModel createContainer(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment) {
        return createContainer(sandboxType, mountDir, storagePath, environment, null, null);
    }

    @RemoteWrapper
    public ContainerModel createContainer(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment, String imageId, Map<String, String> labels) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding createContainer to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxType", sandboxType != null ? sandboxType.name() : null);
            requestData.put("mountDir", mountDir);
            requestData.put("storagePath", storagePath);
            requestData.put("environment", environment);
            requestData.put("imageId", imageId);
            requestData.put("labels", labels);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/createContainer",
                    requestData,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return ContainerModel.fromMap(resultMap);
            }
            return null;
        }

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
        if (containerManagerType == ContainerManagerType.AGENTRUN || containerManagerType == ContainerManagerType.FC) {
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
        if (!mountDir.isEmpty() && !storagePath.isEmpty() && containerManagerType != ContainerManagerType.AGENTRUN && containerManagerType != ContainerManagerType.FC) {
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
        if (containerManagerType != ContainerManagerType.AGENTRUN && containerManagerType != ContainerManagerType.FC) {
            volumeBindings.add(new VolumeBinding(mountDir, workdir, "rw"));
        }
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
        ContainerCreateResult createResult;

        if (sandboxType != SandboxType.AGENTBAY) {
            createResult = containerClient.createContainer(containerName, imageName, ports, volumeBindings, environment, runtimeConfig);
        } else {
            createResult = agentBayClient.createContainer(imageId, labels);
        }

        String containerId = createResult.getContainerId();
        if (containerId == null) {
            logger.severe("Container creation failed: containerId is null");
            return null;
        }
        List<String> resultPorts = createResult.getPorts();
        String ip = createResult.getIp();
        String httpProtocol = createResult.getProtocol();

        String firstPort = resultPorts != null && !resultPorts.isEmpty() ? resultPorts.get(0) : "80";

        String baseHost = ip != null ? ip : "localhost";
        String accessPort = firstPort;

        String[] mappedPorts = resultPorts != null ? resultPorts.toArray(new String[0]) : new String[]{firstPort};

        ContainerModel containerModel = ContainerModel.builder()
                .sessionId(sessionId)
                .containerId(containerId)
                .containerName(containerName)
                .baseUrl(String.format("%s://%s:%s/fastapi", httpProtocol, baseHost, accessPort))
                .browserUrl(String.format("%s://%s:%s/steel-api/%s", httpProtocol, baseHost, accessPort, runtimeToken))
                .frontBrowserWS(String.format("ws://%s:%s/steel-api/%s/v1/sessions/cast", baseHost, accessPort, runtimeToken))
                .clientBrowserWS(String.format("ws://%s:%s/steel-api/%s/&sessionId=%s", baseHost, accessPort, runtimeToken, BROWSER_SESSION_ID))
                .artifactsSIO(String.format("%s://%s:%s/v1", httpProtocol, baseHost, accessPort))
                .ports(mappedPorts)
                .mountDir(mountDir)
                .storagePath(storagePath)
                .runtimeToken(runtimeToken)
                .authToken(runtimeToken)
                .version(imageName)
                .build();

        logger.info("Container Model: " + containerModel);

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

    public ContainerModel getSandbox(SandboxType sandboxType, String userID, String sessionID) {
        return getSandbox(sandboxType, null, null, null, userID, sessionID, null, null);
    }

    public ContainerModel getSandbox(SandboxType sandboxType, String userID, String sessionID, String imageId, Map<String, String> labels) {
        return getSandbox(sandboxType, null, null, null, userID, sessionID, imageId, labels);
    }

    public ContainerModel getSandbox(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment, String userID, String sessionID) {
        return getSandbox(sandboxType, mountDir, storagePath, environment, userID, sessionID, null, null);
    }

    public ContainerModel getSandbox(SandboxType sandboxType, String mountDir, String storagePath, Map<String, String> environment, String userID, String sessionID, String imageId, Map<String, String> labels) {
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
            ContainerModel containerModel = createContainer(sandboxType, mountDir, storagePath, environment, imageId, labels);
            sandboxMap.put(key, containerModel);

            if (redisEnabled && redisContainerMapping != null) {
                redisContainerMapping.put(key, containerModel);
                logger.info("Stored container in Redis: " + containerModel.getContainerName());
            }

            startSandbox(sandboxType, userID, sessionID);
            return containerModel;
        }
    }

    @RemoteWrapper
    public void startSandbox(SandboxType sandboxType, String userID, String sessionID) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding startSandbox to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxType", sandboxType != null ? sandboxType.name() : null);
            requestData.put("userID", userID);
            requestData.put("sessionID", sessionID);
            remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/startSandbox",
                    requestData,
                    "data"
            );
            return;
        }

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

    @RemoteWrapper
    public void stopSandbox(SandboxType sandboxType, String userID, String sessionID) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding stopSandbox to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxType", sandboxType != null ? sandboxType.name() : null);
            requestData.put("userID", userID);
            requestData.put("sessionID", sessionID);
            remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/stopSandbox",
                    requestData,
                    "data"
            );
            return;
        }

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

                if (sandboxType == SandboxType.AGENTBAY) {
                    agentBayClient.removeContainer(containerId);
                    logger.info("Deleted AgentBay container: " + containerName);
                } else {
                    portManager.releaseContainerPorts(containerName);
                    containerClient.stopContainer(containerId);
                    Thread.sleep(1000);
                    containerClient.removeContainer(containerId);
                }
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

    @RemoteWrapper
    public String getSandboxStatus(SandboxType sandboxType, String userID, String sessionID) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding getSandboxStatus to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxType", sandboxType != null ? sandboxType.name() : null);
            requestData.put("userID", userID);
            requestData.put("sessionID", sessionID);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/getSandboxStatus",
                    requestData,
                    "data"
            );
            return result != null ? result.toString() : null;
        }

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

                allSandboxes.putAll(redisData);
            } catch (Exception e) {
                logger.warning("Failed to retrieve containers from Redis: " + e.getMessage());
            }
        }

        return allSandboxes;
    }

    @RemoteWrapper
    public ContainerModel getInfo(String identity) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding getInfo to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("identity", identity);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/getInfo",
                    requestData,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return ContainerModel.fromMap(resultMap);
            }
            return null;
        }

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


    @RemoteWrapper
    public boolean release(String identity) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding release to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("identity", identity);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/release",
                    requestData,
                    "data"
            );
            return result instanceof Boolean ? (Boolean) result : false;
        }

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

    public RemoteHttpClient getRemoteHttpClient() {
        return remoteHttpClient;
    }

    @RemoteWrapper
    public void cleanupAllSandboxes() {
        logger.info("Starting cleanup of all sandbox containers...");

        if (poolQueue != null) {
            try {
                logger.info("Cleaning up container pool (current size: " + poolQueue.size() + ")");
                while (!poolQueue.isEmpty()) {
                    ContainerModel containerModel = poolQueue.dequeue();
                    if (containerModel != null) {
                        logger.info("Destroying pool container: " + containerModel.getContainerName());
                        release(containerModel.getContainerId());
                    }
                }
                logger.info("Container pool cleanup complete");
            } catch (Exception e) {
                logger.severe("Error cleaning up container pool: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.info("Remote mode: no local pool to cleanup");
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

    public String listTools(String identity, String toolType) {
        return "";
    }

    private SandboxClient establishConnection(String sandboxId) {
        try {
            ContainerModel containerInfo = getInfo(sandboxId);
            if (containerInfo.getVersion().contains("sandbox-appworld") || containerInfo.getVersion().contains("sandbox-bfcl")) {
                return new TrainingSandboxClient(containerInfo, 60);
            }
            return new SandboxHttpClient(containerInfo, 60);
        } catch (Exception e) {
            logger.severe("Failed to establish connection to sandbox: " + e.getMessage());
            throw new RuntimeException("Failed to establish connection", e);
        }
    }

    @RemoteWrapper
    public Map<String, Object> listTools(String sandboxId, String userId, String sessionId, String toolType) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding listTools to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxId", sandboxId);
            requestData.put("userId", userId);
            requestData.put("sessionId", sessionId);
            requestData.put("toolType", toolType);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/listTools",
                    requestData,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return resultMap;
            }
            return new HashMap<>();
        }

        try (SandboxClient client = establishConnection(sandboxId)) {
            return client.listTools(toolType, Map.of());
        } catch (Exception e) {
            logger.severe("Error listing tools: " + e.getMessage());
            return new HashMap<>();
        }
    }

    @RemoteWrapper
    public String callTool(String sandboxId, String userId, String sessionId, String toolName, Map<String, Object> arguments) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding callTool to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxId", sandboxId);
            requestData.put("userId", userId);
            requestData.put("sessionId", sessionId);
            requestData.put("toolName", toolName);
            requestData.put("arguments", arguments);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/callTool",
                    requestData,
                    "data"
            );
            return result != null ? result.toString() : null;
        }

        try (SandboxClient client = establishConnection(sandboxId)) {
            return client.callTool(toolName, arguments);
        } catch (Exception e) {
            logger.severe("Error calling tool " + toolName + ": " + e.getMessage());
            e.printStackTrace();
            return "{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"Error calling tool: " + e.getMessage() + "\"}]}";
        }
    }

    @RemoteWrapper
    public Map<String, Object> addMcpServers(String sandboxId, String userId, String sessionId, Map<String, Object> serverConfigs, boolean overwrite) {
        if (remoteHttpClient != null && remoteHttpClient.isConfigured()) {
            logger.info("Remote mode: forwarding addMcpServers to remote server");
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("sandboxId", sandboxId);
            requestData.put("userId", userId);
            requestData.put("sessionId", sessionId);
            requestData.put("serverConfigs", serverConfigs);
            requestData.put("overwrite", overwrite);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/addMcpServers",
                    requestData,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return resultMap;
            }
            return new HashMap<>();
        }

        try (SandboxClient client = establishConnection(sandboxId)) {
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