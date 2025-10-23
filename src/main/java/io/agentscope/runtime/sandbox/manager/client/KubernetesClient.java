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

import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Kubernetes container management client implementation
 * Implements container management functionality based on Kubernetes Java client
 */
public class KubernetesClient extends BaseClient {

    private static final Logger logger = Logger.getLogger(KubernetesClient.class.getName());
    private static final String DEFAULT_NAMESPACE = "default";

    private ApiClient apiClient;
    private CoreV1Api coreApi;
    private AppsV1Api appsApi;
    private boolean connected = false;
    private String kubeconfigPath = null;
    private KubernetesClientConfig config;
    private String namespace;

    /**
     * Default constructor
     */
    public KubernetesClient() {
        // Use default configuration
    }

    /**
     * Constructor specifying kubeconfig file path
     *
     * @param kubeconfigPath kubeconfig file path
     */
    public KubernetesClient(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
        this.namespace = DEFAULT_NAMESPACE;
    }

    /**
     * Constructor using KubernetesClientConfig configuration
     *
     * @param config Kubernetes client configuration
     */
    public KubernetesClient(KubernetesClientConfig config) {
        this.config = config;
        this.kubeconfigPath = config.getKubeConfigPath();
        this.namespace = config.getNamespace() != null ? config.getNamespace() : DEFAULT_NAMESPACE;
    }

    @Override
    public boolean connect() {
        try {
            // Validate configuration
            validateConfig();

            // Choose connection method based on whether kubeconfig file path is specified
            if (kubeconfigPath != null && !kubeconfigPath.trim().isEmpty()) {
                // Load configuration from specified kubeconfig file
                File kubeconfigFile = new File(kubeconfigPath);
                if (!kubeconfigFile.exists()) {
                    throw new RuntimeException("Kubeconfig file not found: " + kubeconfigPath);
                }
                logger.info("Loading Kubernetes configuration from file: " + kubeconfigPath);
                this.apiClient = Config.fromConfig(kubeconfigPath);
            } else {
                // Use default configuration to connect to Kubernetes cluster
                logger.info("Using default Kubernetes configuration");
                this.apiClient = Config.defaultClient();
            }
            Configuration.setDefaultApiClient(this.apiClient);

            // Initialize API clients
            this.coreApi = new CoreV1Api();
            this.appsApi = new AppsV1Api();

            try {
                this.coreApi.getAPIResources().execute();
                logger.info("Successfully connected to Kubernetes cluster");
                this.connected = true;
                return true;
            } catch (Exception e) {
                logger.warning("API resources check failed, trying alternative connection test: " + e.getMessage());

                // Use kubectl command for connection test
                ProcessBuilder processBuilder = new ProcessBuilder("kubectl", "cluster-info");
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    logger.info("Successfully connected to Kubernetes cluster via kubectl");
                    this.connected = true;
                    return true;
                } else {
                    throw new RuntimeException("kubectl cluster-info failed with exit code: " + exitCode);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to connect to Kubernetes: " + e.getMessage());
            this.connected = false;
            return false;
        }
    }

    /**
     * Connect using specified kubeconfig file path
     *
     * @param kubeconfigPath kubeconfig file path
     * @return whether connection is successful
     */
    public boolean connect(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
        return connect();
    }

    @Override
    public boolean isConnected() {
        return connected && apiClient != null;
    }

    /**
     * Get the currently used kubeconfig file path
     *
     * @return kubeconfig file path, returns null if using default configuration
     */
    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    /**
     * Validate configuration
     */
    private void validateConfig() {
        if (config != null) {
            // Validate namespace
            if (config.getNamespace() != null && config.getNamespace().trim().isEmpty()) {
                throw new IllegalArgumentException("Namespace cannot be empty");
            }

            // Validate kubeconfig path
            if (config.getKubeConfigPath() != null && !config.getKubeConfigPath().trim().isEmpty()) {
                File kubeconfigFile = new File(config.getKubeConfigPath());
                if (!kubeconfigFile.exists()) {
                    throw new IllegalArgumentException("Kubeconfig file does not exist: " + config.getKubeConfigPath());
                }
                if (!kubeconfigFile.isFile()) {
                    throw new IllegalArgumentException("Kubeconfig path is not a file: " + config.getKubeConfigPath());
                }
            }

            logger.info("Kubernetes configuration validated successfully");
            logger.info("Using namespace: " + namespace);
            if (kubeconfigPath != null) {
                logger.info("Using kubeconfig: " + kubeconfigPath);
            } else {
                logger.info("Using default kubeconfig");
            }
        }
    }

    @Override
    public String createContainer(String containerName, String imageName,
            List<String> ports, Map<String, Integer> portMapping,
            List<VolumeBinding> volumeBindings,
            Map<String, String> environment, Map<String, Object> runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        String deploymentName = containerName;
        String serviceName = containerName + "-svc"; // Service name

        try {
            // Step 1: Create Deployment
            V1Deployment deployment = createDeploymentObject(deploymentName, imageName,
                    ports, portMapping, volumeBindings, environment, runtimeConfig);
            V1Deployment createdDeployment = appsApi.createNamespacedDeployment(namespace, deployment).execute();
            logger.info("Deployment created: " + createdDeployment.getMetadata().getName());

            // Step 2: If there is portMapping, create LoadBalancer Service
            if (portMapping != null && !portMapping.isEmpty()) {
                V1Service service = createLoadBalancerServiceObject(serviceName, deploymentName, portMapping);
                V1Service createdService = coreApi.createNamespacedService(namespace, service).execute();
                logger.info("LoadBalancer Service created: " + createdService.getMetadata().getName() +
                        ", ports: " + createdService.getSpec().getPorts().stream()
                                .map(p -> p.getPort() + "->" + p.getTargetPort())
                                .toList());
            }

            return deploymentName; // Return Deployment name as container ID

        } catch (ApiException e) {
            logger.severe("Failed to create container (Deployment/Service): " + e.getMessage());
            try {
                appsApi.deleteNamespacedDeployment(deploymentName, namespace).execute();
                coreApi.deleteNamespacedService(serviceName, namespace).execute();
                logger.info("Rolled back resources for: " + containerName);
            } catch (Exception cleanupEx) {
                logger.warning("Failed to rollback after createContainer error: " + cleanupEx.getMessage());
            }
            throw new RuntimeException("Failed to create container", e);
        }
    }

    /**
     * Create Deployment (more advanced controller)
     */
    public String createDeployment(String deploymentName, String imageName,
            List<String> ports, Map<String, Integer> portMapping,
            List<VolumeBinding> volumeBindings,
            Map<String, String> environment, Map<String, Object> runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            V1Deployment deployment = createDeploymentObject(deploymentName, imageName,
                    ports, portMapping, volumeBindings,
                    environment, runtimeConfig);

            V1Deployment createdDeployment = appsApi.createNamespacedDeployment(namespace, deployment).execute();
            logger.info("Deployment created successfully: " + createdDeployment.getMetadata().getName());
            return createdDeployment.getMetadata().getName();

        } catch (ApiException e) {
            logger.severe("Failed to create deployment: " + e.getMessage());
            throw new RuntimeException("Failed to create deployment", e);
        }
    }

    /**
     * Create LoadBalancer type Service object for exposing container ports to
     * external
     */
    private V1Service createLoadBalancerServiceObject(String serviceName, String appName,
            Map<String, Integer> portMapping) {
        List<V1ServicePort> servicePorts = new ArrayList<>();
        int index = 1;

        for (Map.Entry<String, Integer> entry : portMapping.entrySet()) {
            String containerPortSpec = entry.getKey(); // e.g., "80/tcp"
            Integer servicePort = entry.getValue(); // e.g., 80

            String[] parts = containerPortSpec.split("/");
            int containerPort = Integer.parseInt(parts[0]);
            String protocol = (parts.length > 1) ? parts[1].toUpperCase() : "TCP";

            if (!"TCP".equals(protocol) && !"UDP".equals(protocol)) {
                protocol = "TCP";
            }

            V1ServicePort servicePortObj = new V1ServicePort()
                    .name("port-" + index++)
                    .port(servicePort)
                    .targetPort(new IntOrString(containerPort))
                    .protocol(protocol);

            servicePorts.add(servicePortObj);
        }

        // Service selector needs to match Deployment's Pod labels
        Map<String, String> selector = Collections.singletonMap("app", appName);

        return new V1Service()
                .apiVersion("v1")
                .kind("Service")
                .metadata(new V1ObjectMeta().name(serviceName))
                .spec(new V1ServiceSpec()
                        .type("LoadBalancer")
                        .selector(selector)
                        .ports(servicePorts));
    }

    /**
     * Create Deployment object
     */
    private V1Deployment createDeploymentObject(String deploymentName, String imageName,
            List<String> ports, Map<String, Integer> portMapping,
            List<VolumeBinding> volumeBindings,
            Map<String, String> environment, Map<String, Object> runtimeConfig) {

        // Labels for selector and associated resources
        Map<String, String> labels = Collections.singletonMap("app", deploymentName);

        // Collect all container ports that need to be exposed
        Set<Integer> containerPortNumbers = new HashSet<>();
        List<V1ContainerPort> containerPorts = new ArrayList<>();

        // 1. Extract container ports from portMapping (value is host port, but we only
        // care about container port)
        if (portMapping != null && !portMapping.isEmpty()) {
            for (String containerPortStr : portMapping.keySet()) {
                String[] parts = containerPortStr.split("/");
                int portNum = Integer.parseInt(parts[0]);
                String protocol = (parts.length > 1) ? parts[1].toUpperCase() : "TCP";
                if (containerPortNumbers.add(portNum)) { // add returns true if not duplicate
                    V1ContainerPort port = new V1ContainerPort()
                            .containerPort(portNum)
                            .protocol(protocol);
                    containerPorts.add(port);
                }
            }
        }

        // 2. Supplement other ports from ports list
        if (ports != null) {
            for (String portStr : ports) {
                String[] parts = portStr.split("/");
                int portNum = Integer.parseInt(parts[0]);
                String protocol = (parts.length > 1) ? parts[1].toUpperCase() : "TCP";
                if (containerPortNumbers.add(portNum)) {
                    V1ContainerPort port = new V1ContainerPort()
                            .containerPort(portNum)
                            .protocol(protocol);
                    containerPorts.add(port);
                }
            }
        }

        // Environment variables
        List<V1EnvVar> envVars = new ArrayList<>();
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envVars.add(new V1EnvVar().name(entry.getKey()).value(entry.getValue()));
            }
        }

        // Volume mounts
        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        List<V1Volume> volumes = new ArrayList<>();

        volumeBindings = null;

        // Enable file mounting in Kubernetes using hostPath volumes
        if (volumeBindings != null) {
            for (int i = 0; i < volumeBindings.size(); i++) {
                VolumeBinding binding = volumeBindings.get(i);
                String volumeName = "vol-" + i;

                volumeMounts.add(new V1VolumeMount()
                        .name(volumeName)
                        .mountPath(binding.getContainerPath())
                        .readOnly("ro".equals(binding.getMode())));

                // Use hostPath (note: production environment recommends using PVC)
                V1HostPathVolumeSource hostPath = new V1HostPathVolumeSource()
                        .path(binding.getHostPath())
                        .type("DirectoryOrCreate");

                volumes.add(new V1Volume()
                        .name(volumeName)
                        .hostPath(hostPath));
            }
        }

        // Container definition
        V1Container container = new V1Container()
                .name(deploymentName)
                .image(imageName)
                .ports(containerPorts)
                .env(envVars)
                .volumeMounts(volumeMounts)
                .imagePullPolicy("IfNotPresent");

        // Apply runtime configuration to container
        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            logger.info("Applying runtime configuration to Kubernetes container: " + runtimeConfig);
            container = applyRuntimeConfigToContainer(container, runtimeConfig);
        }

        // Pod specification
        V1PodSpec podSpec = new V1PodSpec()
                .containers(Arrays.asList(container));

        if (!volumes.isEmpty()) {
            podSpec.volumes(volumes);
        }

        // Apply runtime configuration to Pod spec
        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            podSpec = applyRuntimeConfigToPodSpec(podSpec, runtimeConfig);
        }

        // Note: No longer automatically enable hostNetwork!
        // If hostNetwork is really needed (such as performance-sensitive scenarios), it
        // should be explicitly enabled through runtimeConfig

        // Pod template
        V1PodTemplateSpec podTemplate = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(podSpec);

        // Deployment specification
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
                .replicas(1)
                .selector(new V1LabelSelector().matchLabels(labels))
                .template(podTemplate);

        // Final Deployment object
        return new V1Deployment()
                .apiVersion("apps/v1")
                .kind("Deployment")
                .metadata(new V1ObjectMeta().name(deploymentName))
                .spec(deploymentSpec);
    }

    @Override
    public void startContainer(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            V1Deployment deployment = appsApi.readNamespacedDeployment(containerId, namespace).execute();
            Integer replicas = deployment.getStatus().getReplicas();
            Integer readyReplicas = deployment.getStatus().getReadyReplicas();
            logger.info("Deployment " + containerId + " replicas: " + replicas + ", ready: " + readyReplicas);
        } catch (ApiException e) {
            logger.severe("Failed to start container: " + e.getMessage());
            throw new RuntimeException("Failed to start container", e);
        }
    }

    @Override
    public void stopContainer(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            // Delete Deployment
            appsApi.deleteNamespacedDeployment(containerId, namespace).execute();
            logger.info("Deployment " + containerId + " deleted successfully");

            // Also delete corresponding Service
            deleteServiceIfExists(containerId);

        } catch (ApiException e) {
            logger.severe("Failed to stop container: " + e.getMessage());
            throw new RuntimeException("Failed to stop container", e);
        }
    }

    @Override
    public void removeContainer(String containerId) {

        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            // Delete Deployment
            appsApi.deleteNamespacedDeployment(containerId, namespace).execute();
            logger.info("Deployment " + containerId + " deleted successfully");

            // Also delete corresponding Service
            deleteServiceIfExists(containerId);

        } catch (ApiException e) {
            if (e.getCode() == 404) {
                logger.info("Deployment " + containerId + " already deleted");
            } else {
                logger.severe("Failed to remove container: " + e.getMessage());
                throw new RuntimeException("Failed to remove container", e);
            }
        }
    }

    @Override
    public String getContainerStatus(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            // Get Deployment status
            V1Deployment deployment = appsApi.readNamespacedDeployment(containerId, namespace).execute();
            Integer replicas = deployment.getStatus().getReplicas();
            Integer readyReplicas = deployment.getStatus().getReadyReplicas();
            if (readyReplicas != null && replicas != null && readyReplicas.equals(replicas)) {
                return "Running";
            } else if (readyReplicas != null && readyReplicas > 0) {
                return "PartiallyReady";
            } else {
                return "Pending";
            }
        } catch (ApiException e) {
            logger.severe("Failed to get container status: " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Get all Pod list
     */
    public List<V1Pod> listPods() {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            V1PodList podList = coreApi.listNamespacedPod(namespace).execute();
            return podList.getItems();
        } catch (ApiException e) {
            logger.severe("Failed to list pods: " + e.getMessage());
            throw new RuntimeException("Failed to list pods", e);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Get all Deployment list
     */
    public List<V1Deployment> listDeployments() {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        try {
            V1DeploymentList deploymentList = appsApi.listNamespacedDeployment(namespace).execute();
            return deploymentList.getItems();
        } catch (ApiException e) {
            logger.severe("Failed to list deployments: " + e.getMessage());
            throw new RuntimeException("Failed to list deployments", e);
        }
    }

    /**
     * Get LoadBalancer Service's External IP
     *
     * @param containerId container ID (Deployment name)
     * @return External IP address, returns null if not assigned
     */
    public String getLoadBalancerExternalIP(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        String serviceName = containerId + "-svc";

        try {
            V1Service service = coreApi.readNamespacedService(serviceName, namespace).execute();
            V1ServiceStatus status = service.getStatus();

            if (status != null && status.getLoadBalancer() != null) {
                V1LoadBalancerStatus loadBalancer = status.getLoadBalancer();
                List<V1LoadBalancerIngress> ingress = loadBalancer.getIngress();

                if (ingress != null && !ingress.isEmpty()) {
                    V1LoadBalancerIngress firstIngress = ingress.get(0);
                    String externalIP = firstIngress.getIp();
                    if (externalIP != null && !externalIP.isEmpty()) {
                        logger.info("LoadBalancer External IP: " + externalIP);
                        return externalIP;
                    }
                }
            }

            logger.info("LoadBalancer External IP not yet assigned for service: " + serviceName);
            return null;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                logger.warning("Service " + serviceName + " not found");
                return null;
            } else {
                logger.severe("Failed to get LoadBalancer External IP: " + e.getMessage());
                throw new RuntimeException("Failed to get LoadBalancer External IP", e);
            }
        }
    }

    /**
     * Wait for LoadBalancer Service's External IP assignment
     *
     * @param containerId    container ID (Deployment name)
     * @param timeoutSeconds timeout time (seconds)
     * @return External IP address, returns null if timeout
     */
    public String waitForLoadBalancerExternalIP(String containerId, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            String externalIP = getLoadBalancerExternalIP(containerId);
            if (externalIP != null && !externalIP.isEmpty()) {
                return externalIP;
            }

            try {
                Thread.sleep(2000); // Wait 2 seconds before retry
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.warning("Timeout waiting for LoadBalancer External IP for container: " + containerId);
        return null;
    }

    /**
     * Apply runtime configuration to container
     *
     * @param container     V1Container to apply configuration to
     * @param runtimeConfig runtime configuration map
     * @return updated V1Container
     */
    private V1Container applyRuntimeConfigToContainer(V1Container container, Map<String, Object> runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.isEmpty()) {
            return container;
        }

        // Create or get existing ResourceRequirements
        V1ResourceRequirements resources = container.getResources();
        if (resources == null) {
            resources = new V1ResourceRequirements();
        }

        Map<String, io.kubernetes.client.custom.Quantity> limits = resources.getLimits();
        Map<String, io.kubernetes.client.custom.Quantity> requests = resources.getRequests();

        if (limits == null) {
            limits = new HashMap<>();
        }
        if (requests == null) {
            requests = new HashMap<>();
        }

        // Handle memory limit (mem_limit)
        if (runtimeConfig.containsKey("mem_limit")) {
            Object memLimitObj = runtimeConfig.get("mem_limit");
            Long memoryBytes = parseMemoryLimit(memLimitObj);
            if (memoryBytes != null) {
                io.kubernetes.client.custom.Quantity memoryQuantity = new io.kubernetes.client.custom.Quantity(
                        String.valueOf(memoryBytes));
                limits.put("memory", memoryQuantity);
                // Set request to same as limit for guaranteed QoS
                requests.put("memory", memoryQuantity);
                logger.info("Applied memory limit: " + memoryBytes + " bytes");
            }
        }

        // Handle CPU limit (nano_cpus)
        // In Kubernetes, CPU is measured in "cores". 1 core = 1,000,000,000 nanocores
        if (runtimeConfig.containsKey("nano_cpus")) {
            Object nanoCpusObj = runtimeConfig.get("nano_cpus");
            Long nanoCpus = parseNanoCpus(nanoCpusObj);
            if (nanoCpus != null) {
                // Convert nanocpus to millicores: 1,000,000,000 nano = 1 core = 1000m
                long milliCpus = nanoCpus / 1_000_000;
                io.kubernetes.client.custom.Quantity cpuQuantity = new io.kubernetes.client.custom.Quantity(
                        milliCpus + "m");
                limits.put("cpu", cpuQuantity);
                // Set request to same as limit for guaranteed QoS
                requests.put("cpu", cpuQuantity);
                logger.info("Applied CPU limit: " + nanoCpus + " nanocpus (" + milliCpus + " millicores)");
            }
        }

        // Handle GPU support (enable_gpu)
        if (runtimeConfig.containsKey("enable_gpu")) {
            Object enableGpuObj = runtimeConfig.get("enable_gpu");
            boolean enableGpu = parseBoolean(enableGpuObj);
            if (enableGpu) {
                // Request GPU resources (nvidia.com/gpu)
                io.kubernetes.client.custom.Quantity gpuQuantity = new io.kubernetes.client.custom.Quantity("1");
                limits.put("nvidia.com/gpu", gpuQuantity);
                requests.put("nvidia.com/gpu", gpuQuantity);
                logger.info("Applied GPU support: enabled (1 GPU requested)");
            }
        }

        // Handle max connections (max_connections)
        // Note: Kubernetes doesn't directly support ulimits in the same way Docker does
        // This would typically be handled at the OS/node level or via init containers
        if (runtimeConfig.containsKey("max_connections")) {
            Object maxConnectionsObj = runtimeConfig.get("max_connections");
            Integer maxConnections = parseInteger(maxConnectionsObj);
            if (maxConnections != null) {
                logger.info("Max connections configuration noted: " + maxConnections +
                        " (Note: Kubernetes doesn't directly support ulimits, consider using init containers or node-level configuration)");
            }
        }

        // Apply resource requirements to container
        resources.setLimits(limits);
        resources.setRequests(requests);
        container.setResources(resources);

        return container;
    }

    /**
     * Apply runtime configuration to Pod spec
     *
     * @param podSpec       V1PodSpec to apply configuration to
     * @param runtimeConfig runtime configuration map
     * @return updated V1PodSpec
     */
    private V1PodSpec applyRuntimeConfigToPodSpec(V1PodSpec podSpec, Map<String, Object> runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.isEmpty()) {
            return podSpec;
        }

        // Handle GPU support - add node selector
        if (runtimeConfig.containsKey("enable_gpu")) {
            Object enableGpuObj = runtimeConfig.get("enable_gpu");
            boolean enableGpu = parseBoolean(enableGpuObj);
            if (enableGpu) {
                // Add node selector for GPU nodes (if applicable)
                Map<String, String> nodeSelector = podSpec.getNodeSelector();
                if (nodeSelector == null) {
                    nodeSelector = new HashMap<>();
                }
                // Common label for GPU-enabled nodes
                nodeSelector.put("accelerator", "nvidia-gpu");
                podSpec.setNodeSelector(nodeSelector);
                logger.info("Added node selector for GPU-enabled nodes");
            }
        }

        return podSpec;
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
     * Delete Service associated with container (if exists)
     *
     * @param containerId container ID (Deployment name)
     */
    private void deleteServiceIfExists(String containerId) {
        String serviceName = containerId + "-svc";

        try {
            // Try to delete Service
            coreApi.deleteNamespacedService(serviceName, namespace).execute();
            logger.info("Service " + serviceName + " deleted successfully");
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                logger.info("Service " + serviceName + " does not exist, skipping deletion");
            } else {
                logger.warning("Failed to delete service " + serviceName + ": " + e.getMessage());
            }
        } catch (Exception e) {
            logger.warning("Unexpected error while deleting service " + serviceName + ": " + e.getMessage());
        }
    }

    @Override
    public boolean imageExists(String imageName) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        return true;
    }

    @Override
    public boolean pullImage(String imageName) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        return true;
    }

    @Override
    public boolean inspectContainer(String containerIdOrName) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        try {
            // Try to read the Deployment to check if it exists
            appsApi.readNamespacedDeployment(containerIdOrName, namespace).execute();
            return true;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // Deployment does not exist
                return false;
            }
            // Other errors, assume it exists
            logger.warning("Error inspecting container " + containerIdOrName + ": " + e.getMessage());
            return false;
        }
    }

}
