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

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.agentscope.runtime.sandbox.manager.model.container.DockerProp;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * @Author: agentscope
 * @Date: 2021/1/10 15:37
 * @Description: Java API implementation for creating Docker containers
 */
public class DockerClient extends BaseClient {
    Logger logger = Logger.getLogger(DockerClient.class.getName());
    private com.github.dockerjava.api.DockerClient client;
    private boolean connected = false;

    /**
     * Connect to Docker server (Mac default connection method)
     *
     * @return: Docker client
     */
    public com.github.dockerjava.api.DockerClient connectDocker() {
        this.client = openDockerClient();
        this.client.infoCmd().exec();
        this.connected = true;
        return this.client;
    }

    @Override
    public boolean connect() {
        try {
            this.client = openDockerClient();
            this.client.infoCmd().exec();
            this.connected = true;
            return true;
        } catch (Exception e) {
            logger.severe("Failed to connect to Docker: " + e.getMessage());
            this.connected = false;
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected && client != null;
    }

    @Override
    public String createContainer(String containerName, String imageName,
                                  List<String> ports, Map<String, Integer> portMapping,
                                  List<VolumeBinding> volumeBindings,
                                  Map<String, String> environment, Map<String, Object> runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }

        CreateContainerResponse response = createContainers(client, containerName, imageName,
                ports, portMapping, volumeBindings, environment, runtimeConfig);

        return response.getId();
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
        removeContainer(client, containerId);
    }

    @Override
    public String getContainerStatus(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Docker client is not connected");
        }
        return getContainerStatus(client, containerId);
    }

    private static com.github.dockerjava.api.DockerClient openDockerClient() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * Connect to Docker server (specify connection address)
     *
     * @param dockerInstance Docker connection address
     * @return: Docker client
     */
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
     * Create container (using DockerProp configuration, supports port binding)
     *
     * @param client     Docker client
     * @param dockerProp Docker configuration properties
     * @return: CreateContainerResponse
     */
    public CreateContainerResponse createContainers(com.github.dockerjava.api.DockerClient client, DockerProp dockerProp) {
        Map<Integer, Integer> portMap = Optional.ofNullable(dockerProp).map(DockerProp::getPartMap).orElse(new HashMap<>());
        Iterator<Map.Entry<Integer, Integer>> iterator = portMap.entrySet().iterator();
        List<PortBinding> portBindingList = new ArrayList<>();
        List<ExposedPort> exposedPortList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            ExposedPort tcp = ExposedPort.tcp(entry.getKey());
            Ports.Binding binding = Ports.Binding.bindPort(entry.getValue());
            PortBinding ports = new PortBinding(binding, tcp);
            portBindingList.add(ports);
            exposedPortList.add(tcp);
        }

        CreateContainerResponse container = null;
        if (dockerProp != null) {
            container = client.createContainerCmd(dockerProp.getImageName())
                    .withName(dockerProp.getContainerName())
                    .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindingList))
                    .withExposedPorts(exposedPortList).exec();
        }

        return container;
    }

    /**
     * Create container (full version, supports port mapping, volume mounting, environment variables, etc.)
     *
     * @param client         Docker client
     * @param containerName  container name
     * @param imageName      image name
     * @param ports          port mapping list, format like ["80/tcp", "443/tcp"]
     * @param volumeBindings volume mount mapping
     * @param environment    environment variables
     * @param runtimeConfig  runtime configuration
     * @return: CreateContainerResponse
     */
    public CreateContainerResponse createContainers(com.github.dockerjava.api.DockerClient client, String containerName, String imageName,
                                                    List<String> ports, Map<String, String> volumeBindings,
                                                    Map<String, String> environment, String runtimeConfig) {

        CreateContainerCmd createCmd = client.createContainerCmd(imageName)
                .withName(containerName);

        // Set port mapping
        if (ports != null && !ports.isEmpty()) {
            ExposedPort[] exposedPorts = new ExposedPort[ports.size()];

            for (int i = 0; i < ports.size(); i++) {
                String[] portParts = ports.get(i).split("/");
                int port = Integer.parseInt(portParts[0]);
                String protocol = portParts.length > 1 ? portParts[1] : "tcp";

                ExposedPort exposedPort = ExposedPort.tcp(port);
                if ("udp".equals(protocol)) {
                    exposedPort = ExposedPort.udp(port);
                }
                exposedPorts[i] = exposedPort;
            }

            createCmd.withExposedPorts(exposedPorts);
        }

        // Set volume mounting
        if (volumeBindings != null && !volumeBindings.isEmpty()) {
            Volume[] volumes = new Volume[volumeBindings.size()];
            Bind[] binds = new Bind[volumeBindings.size()];

            int index = 0;
            for (Map.Entry<String, String> entry : volumeBindings.entrySet()) {
                String hostPath = entry.getKey();
                String containerPath = entry.getValue();

                Volume volume = new Volume(containerPath);
                volumes[index] = volume;
                binds[index] = new Bind(hostPath, volume);
                index++;
            }

            // Use HostConfig to set up volume mounting (recommended way)
            HostConfig hostConfig = new HostConfig()
                    .withBinds(binds);

            createCmd.withVolumes(volumes);
            createCmd.withHostConfig(hostConfig);
        }

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

        // Set runtime configuration
        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            // Note: the withRuntime method may not be available in the current version
            // This parameter is reserved for future expansion
            logger.info("Runtime configuration: " + runtimeConfig + " (not supported in the current version)");
        }

        return createCmd.exec();
    }

    /**
     * Create container (using VolumeBinding format)
     *
     * @param client         Docker client
     * @param containerName  container name
     * @param imageName      image name
     * @param ports          port mapping list, format like ["80/tcp", "443/tcp"]
     * @param volumeBindings volume mount mapping, using VolumeBinding object
     * @param environment    environment variables
     * @param runtimeConfig  runtime configuration
     * @return
     */
    public CreateContainerResponse createContainers(com.github.dockerjava.api.DockerClient client, String containerName, String imageName,
                                                    List<String> ports, List<VolumeBinding> volumeBindings,
                                                    Map<String, String> environment, String runtimeConfig) {

        CreateContainerCmd createCmd = client.createContainerCmd(imageName)
                .withName(containerName);

        // Set port mapping
        if (ports != null && !ports.isEmpty()) {
            ExposedPort[] exposedPorts = new ExposedPort[ports.size()];

            for (int i = 0; i < ports.size(); i++) {
                String[] portParts = ports.get(i).split("/");
                int port = Integer.parseInt(portParts[0]);
                String protocol = portParts.length > 1 ? portParts[1] : "tcp";

                ExposedPort exposedPort = ExposedPort.tcp(port);
                if ("udp".equals(protocol)) {
                    exposedPort = ExposedPort.udp(port);
                }
                exposedPorts[i] = exposedPort;
            }

            createCmd.withExposedPorts(exposedPorts);
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

            // Use HostConfig to set up volume mounting (recommended way)
            HostConfig hostConfig = new HostConfig()
                    .withBinds(binds);

            createCmd.withVolumes(volumes);
            createCmd.withHostConfig(hostConfig);
        }

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

        // Set runtime configuration
        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            // Note: the withRuntime method may not be available in the current version
            // This parameter is reserved for future expansion
            logger.info("Runtime configuration: " + runtimeConfig + " (not supported in the current version)");
        }

        return createCmd.exec();
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
            logger.info("Applying runtime configuration: " + runtimeConfig);
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
                logger.info("Applied memory limit: " + memoryLimit + " bytes");
            }
        }

        // Handle CPU limit (nano_cpus)
        if (runtimeConfig.containsKey("nano_cpus")) {
            Object nanoCpusObj = runtimeConfig.get("nano_cpus");
            Long nanoCpus = parseNanoCpus(nanoCpusObj);
            if (nanoCpus != null) {
                hostConfig = hostConfig.withNanoCPUs(nanoCpus);
                logger.info("Applied nano CPUs: " + nanoCpus);
            }
        }

        // Handle GPU support (enable_gpu)
        if (runtimeConfig.containsKey("enable_gpu")) {
            Object enableGpuObj = runtimeConfig.get("enable_gpu");
            boolean enableGpu = parseBoolean(enableGpuObj);
            if (enableGpu) {
                // Create GPU device request
                DeviceRequest gpuRequest = new DeviceRequest()
                        .withCapabilities(Arrays.asList(Arrays.asList("gpu")))
                        .withCount(-1); // -1 means all GPUs
                hostConfig = hostConfig.withDeviceRequests(Arrays.asList(gpuRequest));
                logger.info("Applied GPU support: enabled");
            }
        }

        // Handle max connections (max_connections) - set via ulimits
        if (runtimeConfig.containsKey("max_connections")) {
            Object maxConnectionsObj = runtimeConfig.get("max_connections");
            Integer maxConnections = parseInteger(maxConnectionsObj);
            if (maxConnections != null) {
                Ulimit ulimit = new Ulimit("nofile", maxConnections.longValue(), maxConnections.longValue());
                hostConfig = hostConfig.withUlimits(Arrays.asList(ulimit));
                logger.info("Applied max connections (nofile limit): " + maxConnections);
            }
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
            // Extract number and unit
            String numberPart = memLimitStr.replaceAll("[^0-9.]", "");
            String unitPart = memLimitStr.replaceAll("[0-9.]", "");

            double value = Double.parseDouble(numberPart);

            // Convert to bytes based on unit
            switch (unitPart) {
                case "k":
                case "kb":
                    return (long) (value * 1024);
                case "m":
                case "mb":
                    return (long) (value * 1024 * 1024);
                case "g":
                case "gb":
                    return (long) (value * 1024 * 1024 * 1024);
                case "t":
                case "tb":
                    return (long) (value * 1024 * 1024 * 1024 * 1024);
                case "":
                    // No unit, assume bytes
                    return (long) value;
                default:
                    logger.warning("Unknown memory unit: " + unitPart);
                    return null;
            }
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse memory limit: " + memLimitStr);
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
            logger.warning("Failed to parse nano CPUs: " + nanoCpusObj);
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
            logger.warning("Failed to parse integer: " + obj);
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
            logger.info("Container started successfully: " + containerId);
        } catch (Exception e) {
            logger.severe("Failed to start container: " + e.getMessage());
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
            // Check container status first
            String status = getContainerStatus(client, containerId);
            if ("running".equals(status)) {
                client.stopContainerCmd(containerId).exec();
                logger.info("Container stopped successfully: " + containerId);
            } else {
                logger.info("Container is already stopped, status: " + status);
            }
        } catch (Exception e) {
            System.err.println("Failed to stop container: " + e.getMessage());
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
            // Force delete the container, even if it is running
            client.removeContainerCmd(containerId)
                    .withForce(true)  // Force delete
                    .withRemoveVolumes(true)  // Also delete associated volumes
                    .exec();
            logger.info("Container deleted successfully: " + containerId);
        } catch (Exception e) {
            System.err.println("Failed to delete container: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Failed to get container status: " + e.getMessage());
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
            // Container does not exist or other error
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
                            logger.info("Image found locally: " + imageName);
                            return true;
                        }
                    }
                }
            }
            logger.info("Image not found locally: " + imageName);
            return false;
        } catch (Exception e) {
            logger.severe("Failed to check if image exists: " + e.getMessage());
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
            logger.info("Pulling image: " + imageName);
            
            PullImageCmd pullCmd = client.pullImageCmd(imageName);
            
            // Add callback to monitor pull progress
            pullCmd.exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.PullResponseItem>() {
                @Override
                public void onNext(com.github.dockerjava.api.model.PullResponseItem item) {
                    if (item.getStatus() != null) {
                        logger.info("Pull progress: " + item.getStatus());
                    }
                    if (item.getErrorDetail() != null) {
                        logger.warning("Pull error: " + item.getErrorDetail().getMessage());
                    }
                }
            }).awaitCompletion();
            
            logger.info("Successfully pulled image: " + imageName);
            return true;
        } catch (Exception e) {
            logger.severe("Failed to pull image " + imageName + ": " + e.getMessage());
            return false;
        }
    }
}
