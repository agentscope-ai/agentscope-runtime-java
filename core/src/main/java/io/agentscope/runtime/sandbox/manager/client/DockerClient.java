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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.util.PortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DockerClient extends BaseClient {
    private static final Logger logger = LoggerFactory.getLogger(DockerClient.class);
    private com.github.dockerjava.api.DockerClient client;
    private boolean connected = false;
    private DockerClientConfig config;
    private PortManager portManager;
    private final Map<String, List<Integer>> portsCache = new ConcurrentHashMap<>();

    /**
     * Default constructor without configuration
     */
    public DockerClient() {
        this.config = null;
        this.portManager = null;
    }

    /**
     * Constructor with DockerClientConfig
     */
    public DockerClient(DockerClientConfig config) {
        this(config, new PortManager(new PortRange()));
    }

    public DockerClient(DockerClientConfig config, PortManager portManager) {
        this.config = config;
        this.portManager = portManager;
    }

    public void connectDocker() {
        this.client = openDockerClient();
        this.client.infoCmd().exec();
        this.connected = true;
    }

    @Override
    public boolean connect() {
        try {
            this.client = openDockerClient();
            this.client.infoCmd().exec();
            this.connected = true;
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to Docker: {}", e.getMessage());
            this.connected = false;
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected && client != null;
    }

    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName,
                                                 List<String> ports,
                                                 List<VolumeBinding> volumeBindings,
                                                 Map<String, String> environment, Map<String, Object> runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }

        // Find free ports automatically if ports are provided
        Map<String, Integer> portMapping = new HashMap<>();
        if (ports != null && !ports.isEmpty()) {
            if (portManager != null) {
                portMapping = findFreePorts(ports);
            } else {
                logger.warn("Ports requested but no PortManager available. Cannot allocate ports.");
            }
        }

        CreateContainerResponse response = createContainers(client, containerName, imageName,
                ports, portMapping, volumeBindings, environment, runtimeConfig);

        String containerId = response.getId();

        // Store port mapping in cache
        if (!portMapping.isEmpty()) {
            List<Integer> hostPorts = new ArrayList<>(portMapping.values());
            portsCache.put(containerId, hostPorts);
            logger.info("Stored port mapping for container {}: {}", containerId, hostPorts);
        }

        // Extract ports from portMapping
        List<String> portList = new ArrayList<>();
        if (!portMapping.isEmpty()) {
            for (Integer port : portMapping.values()) {
                portList.add(String.valueOf(port));
            }
        }

        String ip = "localhost";

        return new ContainerCreateResult(containerId, portList, ip);
    }

    /**
     * Find free ports for the given container ports
     */
    private Map<String, Integer> findFreePorts(List<String> containerPorts) {
        if (portManager == null || containerPorts == null || containerPorts.isEmpty()) {
            return new HashMap<>();
        }

        int[] freePorts = portManager.allocatePorts(containerPorts.size());
        if (freePorts == null) {
            throw new RuntimeException("Not enough free ports available in the specified range.");
        }

        Map<String, Integer> portMapping = new HashMap<>();
        for (int i = 0; i < containerPorts.size() && i < freePorts.length; i++) {
            String containerPort = containerPorts.get(i);
            Integer hostPort = freePorts[i];
            portMapping.put(containerPort, hostPort);
            logger.info("Mapped container port {} to host port {}", containerPort, hostPort);
        }

        return portMapping;
    }

    @Override
    public void startContainer(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        startContainer(client, containerId);
    }

    @Override
    public void stopContainer(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        stopContainer(client, containerId);
    }

    @Override
    public void removeContainer(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }

        // Align with Python version: release ports when container is removed
        List<Integer> ports = portsCache.remove(containerId);
        if (ports != null && portManager != null) {
            for (Integer port : ports) {
                portManager.releasePort(port);
            }
            logger.info("Released {} port(s) for container {}", ports.size(), containerId);
        }

        removeContainer(client, containerId);
    }

    @Override
    public String getContainerStatus(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        return getContainerStatus(client, containerId);
    }

    private com.github.dockerjava.api.DockerClient openDockerClient() {
        if (this.config != null) {
            try {
                DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
                String dockerHost = "tcp://" + this.config.getHost() + ":" + this.config.getPort();
                configBuilder.withDockerHost(dockerHost);

                if (this.config.getCertPath() != null && !this.config.getCertPath().isEmpty()) {
                    configBuilder.withDockerCertPath(this.config.getCertPath());
                    logger.info("Connecting to Docker at {} with TLS", dockerHost);
                } else {
                    logger.info("Connecting to Docker at {} without TLS", dockerHost);
                }

                var config = configBuilder.build();

                DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .build();

                com.github.dockerjava.api.DockerClient client = DockerClientImpl.getInstance(config, httpClient);

                client.infoCmd().exec();
                logger.info("Successfully connected to Docker using configured host and port");
                return client;
            } catch (Exception e) {
                logger.warn("Failed to connect to Docker using configured host ({}:{}): {}", this.config.getHost(), this.config.getPort(), e.getMessage());
                logger.info("Falling back to default Docker configuration");
            }
        }

        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    public com.github.dockerjava.api.DockerClient connectDocker(String dockerInstance) {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerInstance)
                .build();

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        com.github.dockerjava.api.DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        dockerClient.infoCmd().exec();
        return dockerClient;
    }

    /**
     * Create container (simple version)
     *
     * @param client        Docker client
     * @param containerName container name
     * @param imageName     image name
     * @return: CreateContainerResponse
     */
    public CreateContainerResponse createContainers(com.github.dockerjava.api.DockerClient client, String containerName, String imageName) {
        return client.createContainerCmd(imageName)
                .withName(containerName)
                .exec();
    }

    /**
     * Create container (supports port mapping)
     *
     * @param client         Docker client
     * @param containerName  container name
     * @param imageName      image name
     * @param ports          port mapping list, format like ["80/tcp", "443/tcp"]
     * @param portMapping    port mapping Map, key is container port, value is host port
     * @param volumeBindings volume mount mapping, using VolumeBinding object
     * @param environment    environment variables
     * @param runtimeConfig  runtime configuration
     * @return
     */
    public CreateContainerResponse createContainers(com.github.dockerjava.api.DockerClient client, String containerName, String imageName,
                                                    List<String> ports, Map<String, Integer> portMapping,
                                                    List<VolumeBinding> volumeBindings,
                                                    Map<String, String> environment, Map<String, Object> runtimeConfig) {

        CreateContainerCmd createCmd = client.createContainerCmd(imageName)
                .withName(containerName);
        HostConfig hostConfig = HostConfig.newHostConfig();

        // Set port mapping
        if (ports != null && !ports.isEmpty() && portMapping != null && !portMapping.isEmpty()) {
            List<PortBinding> list = new ArrayList<>();
            List<ExposedPort> exposedPorts = new ArrayList<>();

            for (String containerPort : portMapping.keySet()) {
                Integer hostPort = portMapping.get(containerPort);
                // Expose container port
                ExposedPort exposedPort = ExposedPort.parse(containerPort);
                exposedPorts.add(exposedPort);
                // Bind host port -> container port
                list.add(PortBinding.parse(hostPort + ":" + containerPort));
            }

            createCmd = createCmd.withExposedPorts(exposedPorts);
            hostConfig = hostConfig.withPortBindings(list);
        }

        // Set volume mounting (using VolumeBinding)
        if (volumeBindings != null && !volumeBindings.isEmpty()) {
            Volume[] volumes = new Volume[volumeBindings.size()];
            Bind[] binds = new Bind[volumeBindings.size()];

            for (int i = 0; i < volumeBindings.size(); i++) {
                VolumeBinding binding = volumeBindings.get(i);
                Volume volume = new Volume(binding.getContainerPath());
                volumes[i] = volume;

                // Set read-write permissions based on mode
                if ("ro".equals(binding.getMode())) {
                    binds[i] = new Bind(binding.getHostPath(), volume, AccessMode.ro);
                } else {
                    binds[i] = new Bind(binding.getHostPath(), volume, AccessMode.rw);
                }
            }

            hostConfig = hostConfig.withBinds(binds);

            createCmd.withVolumes(volumes);
        }
        createCmd.withHostConfig(hostConfig);

        // Set environment variables
        if (environment != null && !environment.isEmpty()) {
            String[] envArray = new String[environment.size()];
            int index = 0;
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envArray[index] = entry.getKey() + "=" + entry.getValue();
                index++;
            }
            createCmd.withEnv(envArray);
        }

        // Set runtime configuration from runtimeConfig map
        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            logger.info("Applying runtime configuration: {}", runtimeConfig);
            hostConfig = applyRuntimeConfig(hostConfig, runtimeConfig);
            createCmd.withHostConfig(hostConfig);
        }

        return createCmd.exec();
    }


    /**
     * Apply runtime configuration to HostConfig
     *
     * @param hostConfig     current HostConfig
     * @param runtimeConfig  runtime configuration map
     * @return updated HostConfig
     */
    private HostConfig applyRuntimeConfig(HostConfig hostConfig, Map<String, Object> runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.isEmpty()) {
            return hostConfig;
        }

        // Handle memory limit (mem_limit)
        if (runtimeConfig.containsKey("mem_limit")) {
            Object memLimitObj = runtimeConfig.get("mem_limit");
            Long memoryLimit = parseMemoryLimit(memLimitObj);
            if (memoryLimit != null) {
                hostConfig = hostConfig.withMemory(memoryLimit);
                logger.info("Applied memory limit: {} bytes", memoryLimit);
            }
        }

        // Handle CPU limit (nano_cpus)
        if (runtimeConfig.containsKey("nano_cpus")) {
            Object nanoCpusObj = runtimeConfig.get("nano_cpus");
            Long nanoCpus = parseNanoCpus(nanoCpusObj);
            if (nanoCpus != null) {
                hostConfig = hostConfig.withNanoCPUs(nanoCpus);
                logger.info("Applied nano CPUs: {}", nanoCpus);
            }
        }

        // Handle GPU support (enable_gpu)
        if (runtimeConfig.containsKey("enable_gpu")) {
            Object enableGpuObj = runtimeConfig.get("enable_gpu");
            boolean enableGpu = parseBoolean(enableGpuObj);
            if (enableGpu) {
                DeviceRequest gpuRequest = new DeviceRequest()
                        .withCapabilities(List.of(List.of("gpu")))
                        .withCount(-1);
                hostConfig = hostConfig.withDeviceRequests(Collections.singletonList(gpuRequest));
                logger.info("Applied GPU support: enabled");
            }
        }

        // Handle max connections (max_connections)
        if (runtimeConfig.containsKey("max_connections")) {
            Object maxConnectionsObj = runtimeConfig.get("max_connections");
            Integer maxConnections = parseInteger(maxConnectionsObj);
            if (maxConnections != null) {
                Ulimit ulimit = new Ulimit("nofile", maxConnections.longValue(), maxConnections.longValue());
                hostConfig = hostConfig.withUlimits(List.of(ulimit));
                logger.info("Applied max connections (nofile limit): {}", maxConnections);
            }
        }

        // Handle privileged mode (privileged)
        if (runtimeConfig.containsKey("privileged")) {
            Object privilegedObj = runtimeConfig.get("privileged");
            boolean privileged = parseBoolean(privilegedObj);
            hostConfig = hostConfig.withPrivileged(privileged);
            logger.info("Applied privileged mode: {}", privileged);
        }

        return hostConfig;
    }

    /**
     * Parse memory limit from various formats
     * Supports: "4g", "4G", "512m", "512M", 4294967296 (bytes as number)
     *
     * @param memLimitObj memory limit object
     * @return memory limit in bytes, or null if invalid
     */
    private Long parseMemoryLimit(Object memLimitObj) {
        if (memLimitObj == null) {
            return null;
        }

        if (memLimitObj instanceof Number) {
            return ((Number) memLimitObj).longValue();
        }

        String memLimitStr = memLimitObj.toString().trim().toLowerCase();
        if (memLimitStr.isEmpty()) {
            return null;
        }

        try {
            String numberPart = memLimitStr.replaceAll("[^0-9.]", "");
            String unitPart = memLimitStr.replaceAll("[0-9.]", "");

            double value = Double.parseDouble(numberPart);

            return switch (unitPart) {
                case "k", "kb" -> (long) (value * 1024);
                case "m", "mb" -> (long) (value * 1024 * 1024);
                case "g", "gb" -> (long) (value * 1024 * 1024 * 1024);
                case "t", "tb" -> (long) (value * 1024 * 1024 * 1024 * 1024);
                case "" -> (long) value;
                default -> {
                    logger.warn("Unknown memory unit: {}", unitPart);
                    yield null;
                }
            };
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse memory limit: {}", memLimitStr);
            return null;
        }
    }

    /**
     * Parse nano CPUs from various formats
     *
     * @param nanoCpusObj nano CPUs object
     * @return nano CPUs value, or null if invalid
     */
    private Long parseNanoCpus(Object nanoCpusObj) {
        if (nanoCpusObj == null) {
            return null;
        }

        if (nanoCpusObj instanceof Number) {
            return ((Number) nanoCpusObj).longValue();
        }

        try {
            return Long.parseLong(nanoCpusObj.toString());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse nano CPUs: {}", nanoCpusObj);
            return null;
        }
    }

    /**
     * Parse integer from various formats
     *
     * @param obj object to parse
     * @return integer value, or null if invalid
     */
    private Integer parseInteger(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }

        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer: {}", obj);
            return null;
        }
    }

    /**
     * Parse boolean from various formats
     *
     * @param obj object to parse
     * @return boolean value
     */
    private boolean parseBoolean(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }

        String str = obj.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }

    /**
     * Start container
     *
     * @param client      Docker client
     * @param containerId container ID
     */
    public void startContainer(com.github.dockerjava.api.DockerClient client, String containerId) {
        try {
            client.startContainerCmd(containerId).exec();
            logger.info("Container started successfully: {}", containerId);
        } catch (Exception e) {
            logger.error("Failed to start container: {}", e.getMessage());
        }
    }

    /**
     * Stop container
     *
     * @param client      Docker client
     * @param containerId container ID
     */
    public void stopContainer(com.github.dockerjava.api.DockerClient client, String containerId) {
        try {
            String status = getContainerStatus(client, containerId);
            if ("running".equals(status)) {
                client.stopContainerCmd(containerId).exec();
                logger.info("Container stopped successfully: {}", containerId);
            } else {
                logger.info("Container is already stopped, status: {}", status);
            }
        } catch (Exception e) {
            logger.error("Failed to stop container: {}", e.getMessage());
        }
    }

    /**
     * Delete container
     *
     * @param client      Docker client
     * @param containerId container ID
     */
    public void removeContainer(com.github.dockerjava.api.DockerClient client, String containerId) {
        try {
            client.removeContainerCmd(containerId)
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec();
            logger.info("Container deleted successfully: {}", containerId);
        } catch (Exception e) {
            logger.error("Failed to delete container: {}", e.getMessage());
        }
    }

    /**
     * Get container status
     *
     * @param client      Docker client
     * @param containerId container ID
     * @return container status
     */
    public String getContainerStatus(com.github.dockerjava.api.DockerClient client, String containerId) {
        try {
            InspectContainerResponse response = client.inspectContainerCmd(containerId).exec();
            return response.getState().getStatus();
        } catch (Exception e) {
            logger.error("Failed to get container status: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Stop and delete container
     *
     * @param client      Docker client
     * @param containerId container ID
     */
    public void stopAndRemoveContainer(com.github.dockerjava.api.DockerClient client, String containerId) {
        this.stopContainer(client, containerId);
        this.removeContainer(client, containerId);
    }

    @Override
    public boolean imageExists(String imageName) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        return imageExists(client, imageName);
    }

    @Override
    public boolean pullImage(String imageName) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        return pullImage(client, imageName);
    }

    @Override
    public boolean inspectContainer(String containerIdOrName) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        try {
            client.inspectContainerCmd(containerIdOrName).exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if image exists locally
     *
     * @param client     Docker client
     * @param imageName  image name
     * @return whether image exists locally
     */
    public boolean imageExists(com.github.dockerjava.api.DockerClient client, String imageName) {
        try {
            List<Image> images = client.listImagesCmd().exec();
            for (Image image : images) {
                String[] repoTags = image.getRepoTags();
                if (repoTags != null) {
                    for (String repoTag : repoTags) {
                        if (repoTag.equals(imageName)) {
                            logger.info("Image found locally: {}", imageName);
                            return true;
                        }
                    }
                }
            }
            logger.info("Image not found locally: {}", imageName);
            return false;
        } catch (Exception e) {
            logger.error("Failed to check if image exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Pull image from registry
     *
     * @param client     Docker client
     * @param imageName  image name
     * @return whether pull was successful
     */
    public boolean pullImage(com.github.dockerjava.api.DockerClient client, String imageName) {
        try {
            logger.info("Pulling image: {}", imageName);

            PullImageCmd pullCmd = client.pullImageCmd(imageName);

            pullCmd.exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<PullResponseItem>() {
                @Override
                public void onNext(PullResponseItem item) {
                    if (item.getStatus() != null) {
                        logger.info("Pull progress: {}", item.getStatus());
                    }
                    if (item.getErrorDetail() != null) {
                        logger.warn("Pull error: {}", item.getErrorDetail().getMessage());
                    }
                }
            }).awaitCompletion();

            logger.info("Successfully pulled image: {}", imageName);
            return true;
        } catch (Exception e) {
            logger.error("Failed to pull image {}: {}", imageName, e.getMessage());
            return false;
        }
    }
}
