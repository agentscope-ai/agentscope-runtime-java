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
import io.agentscope.runtime.sandbox.manager.model.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.VolumeBinding;
import io.agentscope.runtime.sandbox.manager.model.ContainerManagerType;
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
    private final ContainerManagerType containerManagerType;
    private BaseClient containerClient;
    private final Map<SandboxType, ContainerModel> sandboxMap = new HashMap<>();
    String BROWSER_SESSION_ID = "123e4567-e89b-12d3-a456-426614174000";
    public com.github.dockerjava.api.DockerClient client;
    private BaseClientConfig clientConfig;

    public SandboxManager() {
        this(new DockerClientConfig());
    }
    
    public SandboxManager(ContainerManagerType containerManagerType) {
        this.containerManagerType = containerManagerType;
        logger.info("Initializing SandboxManager with container manager: " + containerManagerType.getValue());
        
        switch (containerManagerType) {
            case DOCKER:
                DockerClient dockerClient = new DockerClient();
                this.containerClient = dockerClient;
                this.client = dockerClient.connectDocker();
                break;
            case KUBERNETES:
                KubernetesClient kubernetesClient = new KubernetesClient("/Users/xht/Downloads/agentscope-runtime-java/kubeconfig.txt");
                this.containerClient = kubernetesClient;
                kubernetesClient.connect();
                this.client = null; // Kubernetes不需要Docker客户端
                break;
            default:
                throw new IllegalArgumentException("Unsupported container manager type: " + containerManagerType);
        }
    }
    
    /**
     * 构造函数，支持Kubernetes配置
     *
     * @param clientConfig 客户端配置
     */
    public SandboxManager(BaseClientConfig clientConfig) {
        this.containerManagerType = clientConfig.getClientType();
        this.clientConfig = clientConfig;
        logger.info("Initializing SandboxManager with container manager: " + containerManagerType.getValue());
        
        switch (containerManagerType) {
            case DOCKER:
                DockerClient dockerClient = new DockerClient();
                this.containerClient = dockerClient;
                this.client = dockerClient.connectDocker();
                break;
            case KUBERNETES:
                if (clientConfig == null) {
                    throw new IllegalArgumentException("KubernetesClientConfig cannot be null for Kubernetes container manager");
                }
                KubernetesClient kubernetesClient;
                if (clientConfig instanceof KubernetesClientConfig) {
                    kubernetesClient = new KubernetesClient((KubernetesClientConfig) clientConfig);
                } else {
                    logger.warning("Provided clientConfig is not an instance of KubernetesClientConfig, using default configuration");
                    kubernetesClient = new KubernetesClient();
                }
                this.containerClient = kubernetesClient;
                kubernetesClient.connect();
                this.client = null; // Kubernetes不需要Docker客户端
                break;
            default:
                throw new IllegalArgumentException("Unsupported container manager type: " + containerManagerType);
        }
    }

    private final Map<SandboxType, String> typeNameMap = new HashMap<>() {{
        put(SandboxType.BASE, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest");
        put(SandboxType.FILESYSTEM, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest");
        put(SandboxType.BROWSER, "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest");
    }};

    public ContainerModel getSandbox(SandboxType sandboxType) {
        if (sandboxMap.containsKey(sandboxType)) {
            return sandboxMap.get(sandboxType);
        } else {
            String workdir = "/workspace";
            String default_mount_dir = "sessions_mount_dir";
            String[] portsArray = {"80/tcp"};
            List<String> ports = Arrays.asList(portsArray);

            // Create port mapping
            Map<String, Integer> portMapping = createPortMapping(ports);
            logger.info("Port mapping: " + portMapping);

            String imageName = "agentscope/runtime-manager-base";
            if (typeNameMap.containsKey(sandboxType)) {
                imageName = typeNameMap.get(sandboxType);
            }

            Map<String, String> environment = new HashMap<>();
            String secretToken = RandomStringGenerator.generateRandomString(16);
            environment.put("SECRET_TOKEN", secretToken);

            System.out.println("Secret Token: " + secretToken);

            String sessionId = secretToken;
            String currentDir = System.getProperty("user.dir");
            String mountDir = currentDir + "/" + default_mount_dir + "/" + sessionId;
            java.io.File file = new java.io.File(mountDir);
            if (!file.exists()) {
                file.mkdirs();
            }

            List<VolumeBinding> volumeBindings = new ArrayList<>();
            volumeBindings.add(new VolumeBinding(
                    mountDir,
                    workdir,
                    "rw"
            ));

            String runtimeConfig = "runc"; // or "nvidia" etc.
            // Kubernetes要求容器名称符合RFC 1123标准：只能包含小写字母、数字、连字符
            String containerName = "sandbox-" + sandboxType.name().toLowerCase() + "-" + sessionId.toLowerCase();

            // 使用统一的BaseClient接口创建容器
            String containerId = containerClient.createContainer(
                    containerName,
                    imageName,
                    ports,
                    portMapping,
                    volumeBindings,
                    environment,
                    runtimeConfig
            );

            // Convert mapped host ports to string array
            String[] mappedPorts = portMapping.values().stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);

            // 根据容器管理器类型确定正确的访问URL
            String baseHost;
            String accessPort;
            
            if (containerManagerType == ContainerManagerType.KUBERNETES) {
                // 在Kubernetes环境中，使用LoadBalancer的External IP
                try {
                    // 等待LoadBalancer分配External IP（最多等待60秒）
                    String externalIP = ((KubernetesClient) containerClient).waitForLoadBalancerExternalIP(containerName, 60);
                    if (externalIP != null && !externalIP.isEmpty()) {
                        baseHost = externalIP;
                        accessPort = "80"; // LoadBalancer默认使用80端口
                        logger.info("Kubernetes LoadBalancer环境: 使用External IP " + externalIP + " 端口 80");
                    } else {
                        logger.warning("无法获取LoadBalancer External IP，使用localhost和映射端口");
                        baseHost = "localhost";
                        accessPort = mappedPorts[0];
                    }
                } catch (Exception e) {
                    logger.warning("获取LoadBalancer External IP失败，使用localhost和映射端口: " + e.getMessage());
                    baseHost = "localhost";
                    accessPort = mappedPorts[0];
                }
            } else {
                // Docker环境使用映射端口
                baseHost = "localhost";
                accessPort = mappedPorts[0];
            }

            ContainerModel containerModel = ContainerModel.builder()
                    .sessionId(sessionId)
                    .containerId(containerId)
                    .containerName(containerName)
                    .baseUrl(String.format("http://%s:%s/fastapi", baseHost, accessPort))
                    .browserUrl(String.format("http://%s:%s/steel-api/%s", baseHost, accessPort, secretToken))
                    .frontBrowserWS(String.format("ws://%s:%s/steel-api/%s/v1/sessions/cast", baseHost, accessPort, secretToken))
                    .clientBrowserWS(String.format("ws://%s:%s/steel-api/%s/&sessionId=%s", baseHost, accessPort, secretToken, BROWSER_SESSION_ID))
                    .artifactsSIO(String.format("http://%s:%s/v1", baseHost, accessPort))
                    .ports(mappedPorts)
                    .mountDir(workdir)
                    .authToken(secretToken)
                    .build();

            sandboxMap.put(sandboxType, containerModel);

            return containerModel;
        }
    }

    /**
     * Start sandbox container
     *
     * @param sandboxType sandbox type
     */
    public void startSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            containerClient.startContainer(containerModel.getContainerId());
            logger.info("Container status updated to: running");
            sandboxMap.put(sandboxType, containerModel);
        }
    }

    /**
     * Stop sandbox container
     *
     * @param sandboxType sandbox type
     */
    public void stopSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            containerClient.stopContainer(containerModel.getContainerId());
            logger.info("Container status updated to: stopped");
            sandboxMap.put(sandboxType, containerModel);
        }
    }

    /**
     * Remove sandbox container
     *
     * @param sandboxType sandbox type
     */
    public void removeSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            containerClient.removeContainer(containerModel.getContainerId());
            sandboxMap.remove(sandboxType);
        }
    }


    public void stopAndRemoveSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
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
                sandboxMap.remove(sandboxType);

                logger.info(sandboxType + " sandbox has been successfully removed");
            } catch (Exception e) {
                System.err.println("Error removing " + sandboxType + " sandbox: " + e.getMessage());
                e.printStackTrace();
                sandboxMap.remove(sandboxType);
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
    public String getSandboxStatus(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
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
    public Map<SandboxType, ContainerModel> getAllSandboxes() {
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
     * Get Kubernetes configuration
     *
     * @return Kubernetes configuration
     */
    public BaseClientConfig getClientConfig() {
        return clientConfig;
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

        for (SandboxType sandboxType : new HashSet<>(sandboxMap.keySet())) {
            try {
                logger.info("Cleaning up " + sandboxType + " sandbox...");
                stopAndRemoveSandbox(sandboxType);
                logger.info(sandboxType + " sandbox cleanup complete");
            } catch (Exception e) {
                logger.severe("Error cleaning up " + sandboxType + " sandbox: " + e.getMessage());
                e.printStackTrace();
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
            if (executor != null && !executor.isShutdown()) {
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
                // 对于Kubernetes LoadBalancer，使用80端口
                for (String containerPort : containerPorts) {
                    portMapping.put(containerPort, 80);
                }
            } else {
                // 对于Docker，使用动态分配的端口
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