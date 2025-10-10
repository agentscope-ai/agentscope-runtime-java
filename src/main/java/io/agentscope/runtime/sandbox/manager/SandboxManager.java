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

    public SandboxManager() {
        this(ContainerManagerType.KUBERNETES);
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
                KubernetesClient kubernetesClient = new KubernetesClient();
                this.containerClient = kubernetesClient;
                kubernetesClient.connect();
                this.client = null; // Kubernetes不需要Docker客户端
                break;
            default:
                throw new IllegalArgumentException("Unsupported container manager type: " + containerManagerType);
        }
    }

    private final Map<SandboxType, String> typeNameMap = new HashMap<>() {{
        put(SandboxType.BASE, "agentscope/runtime-sandbox-base");
//        put(SandboxType.FILESYSTEM, "agentscope/runtime-sandbox-filesystem");
        put(SandboxType.FILESYSTEM, "filesystem");
        put(SandboxType.BROWSER, "agentscope/runtime-sandbox-browser");
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
                // 在Kubernetes环境中，需要获取Service的NodePort
                try {
                    // 获取Service的NodePort
                    String nodePort = getKubernetesServiceNodePort(containerName);
                    baseHost = "localhost";
                    accessPort = nodePort;
                    logger.info("Kubernetes环境: 使用Service NodePort " + nodePort);
                } catch (Exception e) {
                    logger.warning("无法获取Kubernetes Service NodePort，使用默认端口: " + e.getMessage());
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
        int startPort = 30000;
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
            List<Integer> freePorts = findFreePorts(containerPorts.size());

            for (int i = 0; i < containerPorts.size() && i < freePorts.size(); i++) {
                String containerPort = containerPorts.get(i);
                Integer hostPort = freePorts.get(i);
                portMapping.put(containerPort, hostPort);
            }
        }

        return portMapping;
    }

    /**
     * 获取Kubernetes Service的NodePort
     *
     * @param containerName 容器名称
     * @return NodePort端口号
     */
    private String getKubernetesServiceNodePort(String containerName) {
        try {
            // 使用kubectl命令获取Service的NodePort
            ProcessBuilder processBuilder = new ProcessBuilder("kubectl", "get", "service", containerName, "-o", "jsonpath={.spec.ports[0].nodePort}");
            Process process = processBuilder.start();
            
            // 读取输出
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String nodePort = reader.readLine();
            
            int exitCode = process.waitFor();
            if (exitCode == 0 && nodePort != null && !nodePort.isEmpty()) {
                logger.info("获取到Kubernetes Service NodePort: " + nodePort);
                return nodePort;
            } else {
                throw new RuntimeException("无法获取Service NodePort，退出码: " + exitCode);
            }
        } catch (Exception e) {
            logger.severe("获取Kubernetes Service NodePort失败: " + e.getMessage());
            throw new RuntimeException("获取Kubernetes Service NodePort失败", e);
        }
    }

}