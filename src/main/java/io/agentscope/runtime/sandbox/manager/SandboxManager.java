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
import io.agentscope.runtime.sandbox.manager.util.RandomStringGenerator;
import com.github.dockerjava.api.command.CreateContainerResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.logging.Logger;

public class SandboxManager {
    Logger logger = Logger.getLogger(SandboxManager.class.getName());
    DockerClient dockerClient = new DockerClient();
    private final Map<SandboxType, ContainerModel> sandboxMap = new HashMap<>();
    String BROWSER_SESSION_ID="123e4567-e89b-12d3-a456-426614174000";
    public com.github.dockerjava.api.DockerClient client;

    public SandboxManager() {
        logger.info("Initializing SandboxManager and connecting to Docker...");
        client = dockerClient.connectDocker();
    }

    private final Map<SandboxType, String> typeNameMap= new HashMap<>() {{
        put(SandboxType.BASE, "agentscope/runtime-sandbox-base");
        put(SandboxType.FILESYSTEM, "agentscope/runtime-sandbox-filesystem");
        put(SandboxType.BROWSER, "agentscope/runtime-sandbox-browser");
    }};

    public ContainerModel getSandbox(SandboxType sandboxType){
        if(sandboxMap.containsKey(sandboxType)){
            return sandboxMap.get(sandboxType);
        } else {
            DockerClient dockerClient = new DockerClient();
            String workdir = "/workspace";
            String default_mount_dir = "sessions_mount_dir";
            String[] portsArray = {"80/tcp"};
            List<String> ports = Arrays.asList(portsArray);
            
            // Create port mapping
            Map<String, Integer> portMapping = createPortMapping(ports);
            logger.info("Port mapping: " + portMapping);

            String imageName = "agentscope/runtime-manager-base";
            if(typeNameMap.containsKey(sandboxType)){
                imageName = typeNameMap.get(sandboxType);
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
            volumeBindings.add(new VolumeBinding(
                    mountDir,
                    workdir,
                    "rw"
            ));

            String runtimeConfig = "runc"; // or "nvidia" etc.
            String containerName = "sandbox_" + sandboxType.name().toLowerCase() + "_" + sessionId;

            CreateContainerResponse container = dockerClient.createContainers(
                    this.client,
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

            ContainerModel containerModel = ContainerModel.builder()
                    .sessionId(sessionId)
                    .containerId(container.getId())
                    .containerName(containerName)
                    .baseUrl(String.format("http://localhost:%s/fastapi", mappedPorts[0]))
                    .browserUrl(String.format("http://localhost:%s/steel-api/%s", mappedPorts[0], secretToken))
                    .frontBrowserWS(String.format("ws://localhost:%s/steel-api/%s/v1/sessions/cast", mappedPorts[0], secretToken))
                    .clientBrowserWS(String.format("ws://localhost:%s/steel-api/%s/&sessionId=%s", mappedPorts[0], secretToken, BROWSER_SESSION_ID))
                    .artifactsSIO(String.format("http://localhost:%s/v1", mappedPorts[0]))
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
     * @param sandboxType sandbox type
     */
    public void startSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            dockerClient.startContainer(this.client, containerModel.getContainerId());
            logger.info("Container status updated to: running");
            sandboxMap.put(sandboxType, containerModel);
        }
    }

    /**
     * Stop sandbox container
     * @param sandboxType sandbox type
     */
    public void stopSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            dockerClient.stopContainer(this.client, containerModel.getContainerId());
            logger.info("Container status updated to: stopped");
            sandboxMap.put(sandboxType, containerModel);
        }
    }

    /**
     * Remove sandbox container
     * @param sandboxType sandbox type
     */
    public void removeSandbox(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            DockerClient dockerClient = new DockerClient();
            dockerClient.removeContainer(this.client, containerModel.getContainerId());
            sandboxMap.remove(sandboxType);
        }
    }


    public void stopAndRemoveSandbox(SandboxType sandboxType){
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            try {
                DockerClient dockerClient = new DockerClient();
                String containerId = containerModel.getContainerId();
                String containerName = containerModel.getContainerName();

                logger.info("Stopping and removing " + sandboxType + " sandbox (Container ID: " + containerId + ", Name: " + containerName + ")");

                dockerClient.stopContainer(this.client, containerId);
                
                Thread.sleep(1000);
                
                // Force remove the container
                dockerClient.removeContainer(this.client, containerId);
                
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
     * @param sandboxType sandbox type
     * @return sandbox status
     */
    public String getSandboxStatus(SandboxType sandboxType) {
        ContainerModel containerModel = sandboxMap.get(sandboxType);
        if (containerModel != null) {
            DockerClient dockerClient = new DockerClient();
            return dockerClient.getContainerStatus(this.client, containerModel.getContainerId());
        }
        return "not_found";
    }

    /**
     * Get all sandbox information
     * @return sandbox mapping
     */
    public Map<SandboxType, ContainerModel> getAllSandboxes() {
        return new HashMap<>(sandboxMap);
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
     * @param count number of ports to find
     * @return list of available ports
     */
    private List<Integer> findFreePorts(int count) {
        List<Integer> freePorts = new ArrayList<>();
        int startPort = 8000; // Start from 8000
        int maxAttempts = 1000; // Try at most 1000 ports

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
     * @param port port number
     * @return whether the port is available
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Create port mapping
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

}
