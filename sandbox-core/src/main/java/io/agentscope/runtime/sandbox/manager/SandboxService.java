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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.AgentBaySandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClient;
import io.agentscope.runtime.sandbox.manager.client.container.ContainerCreateResult;
import io.agentscope.runtime.sandbox.manager.client.container.agentbay.AgentBayClient;
import io.agentscope.runtime.sandbox.manager.client.sandbox.SandboxClient;
import io.agentscope.runtime.sandbox.manager.client.sandbox.SandboxHttpClient;
import io.agentscope.runtime.sandbox.manager.client.sandbox.TrainingSandboxClient;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.fs.StorageManager;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.model.sandbox.SandboxConfig;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryService;
import io.agentscope.runtime.sandbox.manager.remote.RemoteHttpClient;
import io.agentscope.runtime.sandbox.manager.remote.RemoteWrapper;
import io.agentscope.runtime.sandbox.manager.remote.RequestMethod;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import io.agentscope.runtime.sandbox.manager.utils.RandomStringGenerator;
import io.agentscope.runtime.sandbox.manager.utils.SandboxMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SandboxService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SandboxService.class);
    private final ManagerConfig managerConfig;
    private BaseClient containerClient;
    private final SandboxMap sandboxMap;
    private static final String BROWSER_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private final RemoteHttpClient remoteHttpClient;
    private AgentBayClient agentBayClient;
    private ScheduledExecutorService cleanupExecutor;
    private ScheduledFuture<?> cleanupFuture;

    public SandboxService(ManagerConfig managerConfig) {
        this.managerConfig = managerConfig;
        this.sandboxMap = managerConfig.getSandboxMap();
        if (managerConfig.getBaseUrl() != null && !managerConfig.getBaseUrl().isEmpty()) {
            this.remoteHttpClient = new RemoteHttpClient(managerConfig.getBaseUrl(), managerConfig.getBearerToken());
            logger.info("Initialized SandboxService in remote mode with base URL: {}", managerConfig.getBaseUrl());
        } else {
            this.remoteHttpClient = null;
            logger.info("RemoteHttpClient not initialized: baseUrl is null or empty");
        }
    }

    public void start() {
        logger.info("Initializing SandboxService with container manager: {}", this.managerConfig.getClientStarter().getContainerClientType());
        this.containerClient = this.managerConfig.getClientStarter().startClient(new PortManager(this.managerConfig.getPortRange()));
        if (managerConfig.getAgentBayApiKey() != null && !managerConfig.getAgentBayApiKey().isEmpty()) {
            agentBayClient = new AgentBayClient(managerConfig.getAgentBayApiKey());
        }
        startCleanupTask();
        logger.info("SandboxService started.");
    }

    private void startCleanupTask() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "sandbox-cleanup-task");
            thread.setDaemon(true);
            return thread;
        });
        this.cleanupFuture = this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSandboxes, 5, 5, TimeUnit.SECONDS);
        logger.info("Scheduled cleanup task every 5 seconds");
    }

    private void cleanupExpiredSandboxes() {
        try {
            Map<String, ContainerModel> allSandboxes = sandboxMap.getAllSandboxes();
            for (Map.Entry<String, ContainerModel> entry : allSandboxes.entrySet()) {
                String containerId = entry.getKey();
                long ttl = sandboxMap.getTTL(containerId);
                if (ttl > 0 && ttl < 10) {
                    logger.info("Sandbox {} is expiring (TTL: {}s), removing...", containerId, ttl);
                    removeSandbox(containerId);
                }
            }
        } catch (Exception e) {
            logger.error("Error during scheduled cleanup task: {}", e.getMessage());
        }
    }

    public String createAgentBayContainer(AgentBaySandbox sandbox) {
        if (agentBayClient == null) {
            throw new RuntimeException("AgentBay client is not initialized.");
        }
        ContainerCreateResult createResult = agentBayClient.createContainer(sandbox.getImageId(), sandbox.getLabels());
        String agentBayId = "agentbay_" + sandbox.getImageId() + "_" + sandbox.getLabels().toString();
        ContainerModel containerModel = ContainerModel.builder().containerId(createResult.getContainerId()).containerName(agentBayId).build();
        sandboxMap.addSandbox(new SandboxKey(sandbox.getUserId(), sandbox.getSessionId(), agentBayId), containerModel);
        return createResult.getContainerId();
    }

    private boolean checkSandboxStatus(String userId, String sessionId, String sandboxType){
        String status = getSandboxStatus(userId, sessionId, sandboxType);
        return checkStatusValid(status);
    }

    private boolean checkSandboxStatus(ContainerModel containerModel){
        String status = getSandboxStatus(containerModel);
        return checkStatusValid(status);
    }

    private boolean checkSandboxStatus(String sandboxId){
        String status = getSandboxStatus(sandboxId);
        return checkStatusValid(status);
    }

    private boolean checkStatusValid(String status){
        return status.equalsIgnoreCase("running") ||
                status.equalsIgnoreCase("created") ||
                status.equalsIgnoreCase("partiallyReady") ||
                status.equalsIgnoreCase("pending") ||
                status.equalsIgnoreCase("starting");
    }

    @RemoteWrapper
    public ContainerModel createContainer(Sandbox sandbox) throws JsonProcessingException {
        if (this.remoteHttpClient != null) {
            logger.info("Creating container in remote mode via RemoteHttpClient");
            ObjectMapper mapper = new ObjectMapper();
            String sandboxJson = mapper.writeValueAsString(sandbox);
            Map<String, Object> request = Map.of("sandbox", sandboxJson);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/createContainer",
                    request,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                ContainerModel containerModel = ContainerModel.fromMap(resultMap);
                sandboxMap.addSandbox(new SandboxKey(sandbox.getUserId(), sandbox.getSessionId(), sandbox.getSandboxType()), containerModel);
                return containerModel;
            }
            return null;
        }

        if (sandboxMap.containSandbox(new SandboxKey(sandbox.getUserId(), sandbox.getSessionId(), sandbox.getSandboxType()))) {
            if(checkSandboxStatus(sandbox.getUserId(), sandbox.getSessionId(), sandbox.getSandboxType())){
                ContainerModel existingModel = sandboxMap.getSandbox(new SandboxKey(sandbox.getUserId(), sandbox.getSessionId(), sandbox.getSandboxType()));
                if (existingModel != null) {
                    sandboxMap.incrementRefCount(existingModel.getContainerId());
                }
                return existingModel;
            }
        }

        removeSandbox(sandbox.getUserId(), sandbox.getSessionId(), sandbox.getSandboxType());
        Map<String, String> environment = sandbox.getEnvironment();
        FileSystemConfig fileSystemConfig = sandbox.getFileSystemConfig();
        String sandboxType = sandbox.getSandboxType();
        ContainerClientType containerClientType = managerConfig.getClientStarter().getContainerClientType();
        StorageManager storageManager = fileSystemConfig.createStorageManager();

        String containerName;
        String prefix = managerConfig.getContainerPrefixKey();
        if (prefix == null || prefix.isEmpty()) {
            prefix = "sandbox";
        }

        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                logger.warn("Environment variable {} has null value", entry.getKey());
                return null;
            }
        }

        String workdir = "/workspace";
        String default_mount_dir = fileSystemConfig.getMountDir();
        String[] portsArray = {"80/tcp"};
        List<String> ports = Arrays.asList(portsArray);
        String imageName = SandboxRegistryService.getImageByType(sandboxType).orElse("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest");
        SandboxConfig sandboxConfig = SandboxRegistryService.getConfigByType(sandboxType).orElse(null);
        if (sandboxConfig != null) {
            logger.info("Using registered sandbox configuration: {}", sandboxConfig.getDescription());
            if (sandboxConfig.getEnvironment() != null && !sandboxConfig.getEnvironment().isEmpty()) {
                Map<String, String> mergedEnv = new HashMap<>(sandboxConfig.getEnvironment());
                mergedEnv.putAll(environment);
                environment = mergedEnv;
            }
        }

        logger.info("Checking image: {}", imageName);
        if (containerClientType == ContainerClientType.DOCKER) {
            if (!containerClient.ensureImageAvailable(imageName)) {
                logger.error("Can not get image: {}", imageName);
                throw new RuntimeException("Pull image failed: " + imageName);
            }
            logger.info("Docker image is ready: {}", imageName);
        }

        String sessionId = sandbox.getSessionId();
        String currentDir = System.getProperty("user.dir");
        String mountDir = currentDir + "/" + default_mount_dir + "/" + sessionId;

        if (containerClientType == ContainerClientType.DOCKER) {
            containerName = prefix + sessionId.toLowerCase();
            while(containerClient.containerNameExists(containerName)){
                sessionId = RandomStringGenerator.generateRandomString(22);
                containerName = prefix + sessionId.toLowerCase();
            }
        } else {
            containerName = prefix.replace('_', '-') + sessionId.toLowerCase();
        }

        if (containerClientType == ContainerClientType.AGENTRUN || containerClientType == ContainerClientType.FC) {
            mountDir = Paths.get(mountDir).toAbsolutePath().toString();
        }
        File file = new File(mountDir);
        if (!file.exists()) {
            boolean ignored = file.mkdirs();
        }
        String storagePath = sandbox.getFileSystemConfig().getStorageFolderPath();
        // Todo：Currently using global storage path if not provided, still need to wait for next movement of python version
        if (!mountDir.isEmpty() && !storagePath.isEmpty() && containerClientType != ContainerClientType.AGENTRUN && containerClientType != ContainerClientType.FC) {
            logger.info("Downloading from storage path: {} to mount dir: {}", storagePath, mountDir);
            boolean downloadSuccess = storageManager.downloadFolder(storagePath, mountDir);
            if (downloadSuccess) {
                logger.info("Successfully downloaded files from storage");
            } else {
                logger.warn("Failed to download files from storage, continuing with empty mount dir");
            }
        }
        String runtimeToken = RandomStringGenerator.generateRandomString(32);
        environment.put("SECRET_TOKEN", runtimeToken);
        List<VolumeBinding> volumeBindings = new ArrayList<>();
        if (containerClientType != ContainerClientType.AGENTRUN && containerClientType != ContainerClientType.FC) {
            volumeBindings.add(new VolumeBinding(mountDir, workdir, "rw"));
        }
        Map<String, String> readonlyMounts = fileSystemConfig.getReadonlyMounts();
        if (readonlyMounts != null && !readonlyMounts.isEmpty()) {
            logger.info("Adding readonly mounts: {} mount(s)", readonlyMounts.size());
            for (Map.Entry<String, String> entry : readonlyMounts.entrySet()) {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue();
                if (!Paths.get(hostPath).isAbsolute()) {
                    hostPath = Paths.get(hostPath).toAbsolutePath().toString();
                    logger.info("Converting relative path to absolute: {}", hostPath);
                }
                File hostFile = new File(hostPath);
                if (!hostFile.exists()) {
                    logger.warn("Readonly mount host path does not exist: {}, skipping", hostPath);
                    continue;
                }
                volumeBindings.add(new VolumeBinding(hostPath, containerPath, "ro"));
                logger.info("Added readonly mount: {} -> {}", hostPath, containerPath);
            }
        }
        Map<String, String> nonCopyMounts = fileSystemConfig.getNonCopyMount();
        if (nonCopyMounts != null && !nonCopyMounts.isEmpty()) {
            logger.info("Adding non-copy mounts: {} mount(s)", nonCopyMounts.size());
            for (Map.Entry<String, String> entry : nonCopyMounts.entrySet()) {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue();
                if (!Paths.get(hostPath).isAbsolute()) {
                    hostPath = Paths.get(hostPath).toAbsolutePath().toString();
                    logger.info("Converting relative path to absolute: {}", hostPath);
                }
                File hostFile = new File(hostPath);
                if (!hostFile.exists()) {
                    logger.warn("Host path does not exist: {}, attempting to create", hostPath);
                    try {
                        if (hostPath.endsWith(File.separator) || hostPath.endsWith("/") ||
                                containerPath.endsWith("/") || containerPath.endsWith(File.separator)) {
                            if (hostFile.mkdirs()) {
                                logger.info("Successfully created directory: {}", hostPath);
                            } else {
                                logger.warn("Failed to create directory: {}", hostPath);
                                continue;
                            }
                        } else {
                            File parentDir = hostFile.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                            if (hostFile.createNewFile()) {
                                logger.info("Successfully created file: {}", hostPath);
                            } else {
                                logger.warn("Failed to create file: {}", hostPath);
                                continue;
                            }
                        }
                    } catch (IOException e) {
                        logger.warn("Exception while creating path {}: {}", hostPath, e.getMessage());
                        continue;
                    }
                }
                volumeBindings.add(new VolumeBinding(hostPath, containerPath, "rw"));
                logger.info("Added non Copy mount: {} -> {}", hostPath, containerPath);
            }
        }

        Map<String, Object> runtimeConfig = Map.of();
        if (sandboxConfig != null) {
            runtimeConfig = sandboxConfig.getRuntimeConfig();
        }
        logger.info("Runtime config: {}", runtimeConfig);
        ContainerCreateResult createResult;
        createResult = containerClient.createContainer(containerName, imageName, ports, volumeBindings, environment, runtimeConfig);

        String containerId = createResult.getContainerId();
        if (containerId == null) {
            logger.error("Container creation failed: containerId is null");
            return null;
        }
        List<String> resultPorts = createResult.getPorts();
        String ip = createResult.getIp();
        String httpProtocol = createResult.getProtocol();

        String accessPort = resultPorts != null && !resultPorts.isEmpty() ? resultPorts.get(0) : "80";

        String baseHost = ip != null ? ip : "localhost";

        String[] mappedPorts = resultPorts != null ? resultPorts.toArray(new String[0]) : new String[]{accessPort};

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

        logger.info("Created Container: {}", containerModel);

        containerClient.startContainer(containerId);
        sandboxMap.addSandbox(new SandboxKey(sandbox.getUserId(), sandbox.getSessionId(), sandbox.getSandboxType()), containerModel);
        sandboxMap.incrementRefCount(containerModel.getContainerId());
        sandbox.setSandboxId(containerModel.getContainerId());
        return containerModel;
    }

    public ContainerModel getSandbox(String containerId) {
        return sandboxMap.getSandbox(containerId);
    }

    public ContainerModel getSandbox(String userId, String sessionId, String sandboxType) {
        return sandboxMap.getSandbox(new SandboxKey(userId, sessionId, sandboxType));
    }

    public boolean startSandbox(String userId, String sessionId, String sandboxType) {
        return startSandbox(sandboxMap.getSandbox(new SandboxKey(userId, sessionId, sandboxType)));
    }

    public boolean startSandbox(String containerId) {
        return startSandbox(sandboxMap.getSandbox(containerId));
    }

    @RemoteWrapper
    public boolean startSandbox(ContainerModel containerModel) {
        if (this.remoteHttpClient != null) {
            logger.info("Starting sandbox in remote mode via RemoteHttpClient");
            Map<String, Object> request = Map.of("containerModel", containerModel);
            remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/startSandbox",
                    request,
                    "data"
            );
            return true;
        }
        if (containerModel == null) {
            return false;
        }
        containerClient.startContainer(containerModel.getContainerId());
        logger.info("Container status updated to: running");
        return true;
    }

    public void stopSandbox(String userId, String sessionId, String sandboxType) {
        stopSandbox(sandboxMap.getSandbox(new SandboxKey(userId, sessionId, sandboxType)));
    }

    public void stopSandbox(String containerId) {
        stopSandbox(sandboxMap.getSandbox(containerId));
    }

    @RemoteWrapper
    public void stopSandbox(ContainerModel containerModel) {
        if(containerModel.getContainerName().startsWith("agentbay_")) {
            logger.info("Stopping sandbox managed by AgentBay via AgentBayClient");
            agentBayClient.stopContainer(containerModel.getContainerId());
            return;
        }
        if (this.remoteHttpClient != null) {
            logger.info("Stopping sandbox in remote mode via RemoteHttpClient");
            Map<String, Object> request = Map.of("containerModel", containerModel);
            remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/stopSandbox",
                    request,
                    "data"
            );
            return;
        }
        containerClient.stopContainer(containerModel.getContainerId());
        logger.info("Container status updated to: stopped");
    }

    public boolean removeSandbox(String userId, String sessionId, String sandboxType) {
        return removeSandbox(sandboxMap.getSandbox(new SandboxKey(userId, sessionId, sandboxType)));
    }

    public boolean removeSandbox(String containerId) {
        return removeSandbox(sandboxMap.getSandbox(containerId));
    }

    @RemoteWrapper
    public boolean removeSandbox(ContainerModel containerModel) {
        if(containerModel == null){
            return false;
        }

        sandboxMap.decrementRefCount(containerModel.getContainerId());
        long refCount = sandboxMap.getRefCount(containerModel.getContainerId());
        if (refCount > 0) {
            logger.info("Sandbox {} has active references ({}), skip removing", containerModel.getContainerId(), refCount);
            return true;
        }

        if(containerModel.getContainerName().startsWith("agentbay_")) {
            logger.warn("AgentBay sandbox can only be stopped, not removed via AgentBayClient");
            return true;
        }
        if (this.remoteHttpClient != null) {
            logger.info("Removing sandbox in remote mode via RemoteHttpClient");
            Map<String, Object> request = Map.of("containerModel", containerModel);
            remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/removeSandbox",
                    request,
                    "data"
            );
            return true;
        }
        containerClient.removeContainer(containerModel.getContainerId());
        logger.info("Container removed: {}", containerModel.getContainerId());
        sandboxMap.removeSandbox(containerModel.getContainerId());
        return true;
    }

    public boolean stopAndRemoveSandbox(String userId, String sessionId, String sandboxType) {
        stopSandbox(userId, sessionId, sandboxType);
        return removeSandbox(userId, sessionId, sandboxType);
    }

    public boolean stopAndRemoveSandbox(String containerId) {
        stopSandbox(containerId);
        return removeSandbox(containerId);
    }

    public boolean release(String containerId) {
        if (containerId == null || containerId.isEmpty()) return false;
        return stopAndRemoveSandbox(containerId);
    }

    public String getSandboxStatus(String userId, String sessionId, String sandboxType) {
        return getSandboxStatus(sandboxMap.getSandbox(new SandboxKey(userId, sessionId, sandboxType)));
    }

    public String getSandboxStatus(String containerId) {
        return getSandboxStatus(sandboxMap.getSandbox(containerId));
    }

    @RemoteWrapper
    public String getSandboxStatus(ContainerModel containerModel) {
        if (this.remoteHttpClient != null) {
            logger.info("Getting sandbox status in remote mode via RemoteHttpClient");
            Map<String, Object> request = Map.of("containerModel", containerModel);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/getSandboxStatus",
                    request,
                    "data"
            );
            if (result instanceof String) {
                return (String) result;
            }
            return "unknown";
        }
        if (containerModel == null) {
            return "not_found";
        }
        return containerClient.getContainerStatus(containerModel.getContainerId());
    }

    public Map<String, ContainerModel> getAllSandboxes() {
        return sandboxMap.getAllSandboxes();
    }

    @RemoteWrapper
    public ContainerModel getInfo(Sandbox sandbox) throws JsonProcessingException {
        if (this.remoteHttpClient != null) {
            logger.info("Getting sandbox info in remote mode via RemoteHttpClient");
            ObjectMapper mapper = new ObjectMapper();
            String sandboxJson = mapper.writeValueAsString(sandbox);
            Map<String, Object> request = Map.of("sandbox", sandboxJson);
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/getInfo",
                    request,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return ContainerModel.fromMap(resultMap);
            }
            return null;
        }
        return sandboxMap.getSandbox(sandbox.getSandboxId());
    }

    public void cleanupAllSandboxes() {
        for (String containerId : sandboxMap.getAllSandboxes().keySet()) {
            if (!removeSandbox(containerId)) {
                logger.warn("Error cleaning up container {}", containerId);
            }
        }
    }

    @Override
    public void close() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        cleanupAllSandboxes();
    }

    private SandboxClient establishConnection(Sandbox sandbox) {
        try {
            if(!checkSandboxStatus(sandbox.getSandboxId())){
                createContainer(sandbox);
            }
            ContainerModel containerInfo = getInfo(sandbox);
            if (containerInfo.getVersion().contains("sandbox-appworld") || containerInfo.getVersion().contains("sandbox-bfcl")) {
                return new TrainingSandboxClient(containerInfo, 60);
            }
            return new SandboxHttpClient(containerInfo, 60);
        } catch (Exception e) {
            logger.error("Failed to establish connection to sandbox: {}", e.getMessage());
            throw new RuntimeException("Failed to establish connection", e);
        }
    }

    @RemoteWrapper
    public Map<String, Object> listTools(Sandbox sandbox, String toolType) throws JsonProcessingException {
        if (this.remoteHttpClient != null) {
            logger.info("Listing tools in remote mode via RemoteHttpClient");
            ObjectMapper mapper = new ObjectMapper();
            String sandboxJson = mapper.writeValueAsString(sandbox);
            Map<String, Object> request = Map.of(
                    "sandbox", sandboxJson,
                    "toolType", toolType
            );
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/listTools",
                    request,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return resultMap;
            }
            return new HashMap<>();
        }
        try (SandboxClient client = establishConnection(sandbox)) {
            return client.listTools(toolType, Map.of());
        } catch (Exception e) {
            logger.error("Error listing tools: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @RemoteWrapper
    public String callTool(Sandbox sandbox, String toolName, Map<String, Object> arguments) throws JsonProcessingException {
        if (this.remoteHttpClient != null) {
            logger.info("Calling tool in remote mode via RemoteHttpClient");
            ObjectMapper mapper = new ObjectMapper();
            String sandboxJson = mapper.writeValueAsString(sandbox);
            Map<String, Object> request = Map.of(
                    "sandbox", sandboxJson,
                    "toolName", toolName,
                    "arguments", arguments
            );
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/callTool",
                    request,
                    "data"
            );
            if (result instanceof String) {
                return (String) result;
            }
            return "{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"Invalid response from remote callTool\"}]}";
        }
        try (SandboxClient client = establishConnection(sandbox)) {
            return client.callTool(toolName, arguments);
        } catch (Exception e) {
            logger.error("Error calling tool {}: {}", toolName, e.getMessage());
            return "{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"Error calling tool: " + e.getMessage() + "\"}]}";
        }
    }

    @RemoteWrapper
    public Map<String, Object> addMcpServers(Sandbox sandbox, Map<String, Object> serverConfigs, boolean overwrite) throws JsonProcessingException {
        if (this.remoteHttpClient != null) {
            logger.info("Adding MCP servers in remote mode via RemoteHttpClient");
            ObjectMapper mapper = new ObjectMapper();
            String sandboxJson = mapper.writeValueAsString(sandbox);
            Map<String, Object> request = Map.of(
                    "sandbox", sandboxJson,
                    "serverConfigs", serverConfigs,
                    "overwrite", overwrite
            );
            Object result = remoteHttpClient.makeRequest(
                    RequestMethod.POST,
                    "/sandbox/addMcpServers",
                    request,
                    "data"
            );
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return resultMap;
            }
            return new HashMap<>();
        }
        try (SandboxClient client = establishConnection(sandbox)) {
            return client.addMcpServers(serverConfigs, overwrite);
        } catch (Exception e) {
            logger.error("Error adding MCP servers: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public AgentBayClient getAgentBayClient() {
        return agentBayClient;
    }

    public void stop(){
        cleanupAllSandboxes();
        if (this.cleanupFuture != null) {
            this.cleanupFuture.cancel(true);
        }
    }
}
