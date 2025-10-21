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
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;
import io.agentscope.runtime.sandbox.manager.util.RandomStringGenerator;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SandboxManager {
    Logger logger = Logger.getLogger(SandboxManager.class.getName());
    private ManagerConfig managerConfig;
    private final Map<SandboxKey, ContainerModel> sandboxMap = new HashMap<>();
    String BROWSER_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    public com.github.dockerjava.api.DockerClient client;
    private BaseClient containerClient;
    private final ContainerManagerType containerManagerType;

    public SandboxManager() {
        this(new ManagerConfig.Builder().build());
    }

    /**
     * Constructor supporting Kubernetes configuration
     *
     * @param managerConfig manager configuration
     */
    public SandboxManager(ManagerConfig managerConfig) {
        this.containerManagerType = managerConfig.getClientConfig().getClientType();
        logger.info("Initializing SandboxManager with container manager: " + this.containerManagerType);

        switch (this.containerManagerType) {
            case DOCKER:
                // Todo: 让DockerClient支持DockerClientConfig配置
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
    }

    private final Map<SandboxType, String> typeNameMap = new HashMap<>() {{
        put(SandboxType.BASE, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest");
        put(SandboxType.FILESYSTEM, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest");
        put(SandboxType.BROWSER, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest");
        put(SandboxType.TRAINING, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest");
        put(SandboxType.APPWORLD, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest");
        put(SandboxType.BFCL, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest");
        put(SandboxType.WEBSHOP, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest");
    }};

    public ContainerModel getSandbox(SandboxType sandboxType, String userID, String sessionID) {
        // Use composite key userID + sessionID + sandboxType to uniquely identify container
        SandboxKey key = new SandboxKey(userID, sessionID, sandboxType);
        if (sandboxMap.containsKey(key)) {
            return sandboxMap.get(key);
        } else {
            String workdir = "/workspace";
            String default_mount_dir = "sessions_mount_dir";
            String[] portsArray = {"80/tcp"};
            List<String> ports = Arrays.asList(portsArray);

            // Create port mapping
            Map<String, Integer> portMapping = createPortMapping(ports);
            logger.info("Port mapping: " + portMapping);

            String imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest";
            if (typeNameMap.containsKey(sandboxType)) {
                imageName = typeNameMap.get(sandboxType);
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

            Map<String, String> environment = new HashMap<>();
            String secretToken = RandomStringGenerator.generateRandomString(16);
            environment.put("SECRET_TOKEN", secretToken);

            String sessionId = secretToken;
            String currentDir = System.getProperty("user.dir");
            String mountDir = currentDir + "/" + default_mount_dir + "/" + sessionId;
            java.io.File file = new java.io.File(mountDir);
            if (!file.exists()) {
                file.mkdirs();
            }

            List<VolumeBinding> volumeBindings = new ArrayList<>();
            volumeBindings.add(new VolumeBinding(mountDir, workdir, "rw"));

            String runtimeConfig = "runc"; // or "nvidia" etc.
            String containerName = "sandbox-" + sandboxType.name().toLowerCase() + "-" + sessionId.toLowerCase();

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

            ContainerModel containerModel = ContainerModel.builder().sessionId(sessionId).containerId(containerId).containerName(containerName).baseUrl(String.format("http://%s:%s/fastapi", baseHost, accessPort)).browserUrl(String.format("http://%s:%s/steel-api/%s", baseHost, accessPort, secretToken)).frontBrowserWS(String.format("ws://%s:%s/steel-api/%s/v1/sessions/cast", baseHost, accessPort, secretToken)).clientBrowserWS(String.format("ws://%s:%s/steel-api/%s/&sessionId=%s", baseHost, accessPort, secretToken, BROWSER_SESSION_ID)).artifactsSIO(String.format("http://%s:%s/v1", baseHost, accessPort)).ports(mappedPorts).mountDir(workdir).authToken(secretToken).build();

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
     * Clean up all sandbox containers
     */
    public void cleanupAllSandboxes() {
        if (sandboxMap.isEmpty()) {
            logger.info("No sandbox containers to clean up");
            return;
        }

        logger.info("Starting to clean up all sandbox containers...");
        logger.info("Current number of sandboxes: " + sandboxMap.size());
        logger.info("Sandbox types: " + sandboxMap.keySet());

        for (SandboxKey key : new HashSet<>(sandboxMap.keySet())) {
            try {
                logger.info("Cleaning up " + key.getSandboxType() + " sandbox for user " + key.getUserID() + " session " + key.getSessionID() + "...");
                stopAndRemoveSandbox(key.getSandboxType(), key.getUserID(), key.getSessionID());
                logger.info(key.getSandboxType() + " sandbox cleanup complete");
            } catch (Exception e) {
                logger.severe("Error cleaning up " + key.getSandboxType() + " sandbox: " + e.getMessage());
            }
        }

        sandboxMap.clear();
        logger.info("All sandbox containers have been cleaned up!");
    }

    /**
     * Find available ports
     *
     * @param count number of ports to find
     * @return list of available ports
     */
    private List<Integer> findFreePorts(int count) {
        List<Integer> freePorts = new ArrayList<>();
        int startPort = 30004;
        int maxAttempts = 2000;
        for (int i = 0; i < count && freePorts.size() < count; i++) {
            for (int port = startPort + i; port < startPort + maxAttempts; port++) {
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

    /**
     * Create port mapping
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
                // For Docker, use dynamically assigned ports
                // Todo: Current implementation still only considers local Docker environment
                List<Integer> freePorts = findFreePorts(containerPorts.size());
                for (int i = 0; i < containerPorts.size() && i < freePorts.size(); i++) {
                    String containerPort = containerPorts.get(i);
                    Integer hostPort = freePorts.get(i);
                    portMapping.put(containerPort, hostPort);
                }
            }
        }

        return portMapping;
    }


}