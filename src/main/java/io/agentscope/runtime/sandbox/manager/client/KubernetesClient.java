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

    public KubernetesClient() {
    }

    public KubernetesClient(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
        this.namespace = DEFAULT_NAMESPACE;
    }

    public KubernetesClient(KubernetesClientConfig config) {
        this.config = config;
        this.kubeconfigPath = config.getKubeConfigPath();
        this.namespace = config.getNamespace() != null ? config.getNamespace() : DEFAULT_NAMESPACE;
    }

    @Override
    public boolean connect() {
        try {
            validateConfig();

            if (kubeconfigPath != null && !kubeconfigPath.trim().isEmpty()) {
                File kubeconfigFile = new File(kubeconfigPath);
                if (!kubeconfigFile.exists()) {
                    throw new RuntimeException("Kubeconfig file not found: " + kubeconfigPath);
                }
                logger.info("Loading Kubernetes configuration from file: " + kubeconfigPath);
                this.apiClient = Config.fromConfig(kubeconfigPath);
            } else {
                logger.info("Using default Kubernetes configuration");
                this.apiClient = Config.defaultClient();
            }
            Configuration.setDefaultApiClient(this.apiClient);

            this.coreApi = new CoreV1Api();
            this.appsApi = new AppsV1Api();

            try {
                this.coreApi.getAPIResources().execute();
                logger.info("Successfully connected to Kubernetes cluster");
                this.connected = true;
                return true;
            } catch (Exception e) {
                logger.warning("API resources check failed, trying alternative connection test: " + e.getMessage());

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
     * Connect using kubeconfig file path
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

    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    private void validateConfig() {
        if (config != null) {
            if (config.getNamespace() != null && config.getNamespace().trim().isEmpty()) {
                throw new IllegalArgumentException("Namespace cannot be empty");
            }

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
    public ContainerCreateResult createContainer(String containerName, String imageName,
                                  List<String> ports,
                                  List<VolumeBinding> volumeBindings,
                                  Map<String, String> environment, Map<String, Object> runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        String deploymentName = containerName;
        String serviceName = containerName + "-svc"; // Service name

        try {
            // KubernetesClient handles port mapping internally if needed
            // For LoadBalancer, use port 80 as default (aligns with Python version behavior)
            Map<String, Integer> portMapping = new HashMap<>();
            if (ports != null && !ports.isEmpty()) {
                for (String containerPort : ports) {
                    portMapping.put(containerPort, 80);
                }
            }

            V1Deployment deployment = createDeploymentObject(deploymentName, imageName,
                    ports, portMapping, volumeBindings, environment, runtimeConfig);
            V1Deployment createdDeployment = appsApi.createNamespacedDeployment(namespace, deployment).execute();
            logger.info("Deployment created: " + createdDeployment.getMetadata().getName());

            List<String> exposedPorts = new ArrayList<>();
            String serviceIp = "localhost";
            
            if (!portMapping.isEmpty()) {
                V1Service service = createLoadBalancerServiceObject(serviceName, deploymentName, portMapping);
                V1Service createdService = coreApi.createNamespacedService(namespace, service).execute();
                logger.info("LoadBalancer Service created: " + createdService.getMetadata().getName() +
                        ", ports: " + createdService.getSpec().getPorts().stream()
                        .map(p -> p.getPort() + "->" + p.getTargetPort())
                        .toList());
                
                // Get exposed ports from portMapping
                for (Integer port : portMapping.values()) {
                    exposedPorts.add(String.valueOf(port));
                }
                
                // Try to get LoadBalancer External IP (preferred)
                try {
                    String externalIP = waitForLoadBalancerExternalIP(deploymentName, 60);
                    if (externalIP != null && !externalIP.isEmpty()) {
                        serviceIp = externalIP;
                        logger.info("Kubernetes LoadBalancer environment: using External IP " + externalIP);
                    } else {
                        logger.warning("Unable to get LoadBalancer External IP, trying pod node IP");
                        // Fallback to pod node IP
                        try {
                            serviceIp = getPodNodeIp(deploymentName);
                            if (serviceIp == null || serviceIp.isEmpty()) {
                                serviceIp = "localhost";
                            }
                        } catch (Exception e) {
                            logger.warning("Failed to get pod node IP, using localhost: " + e.getMessage());
                            serviceIp = "localhost";
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Failed to get LoadBalancer External IP, trying pod node IP: " + e.getMessage());
                    // Fallback to pod node IP
                    try {
                        serviceIp = getPodNodeIp(deploymentName);
                        if (serviceIp == null || serviceIp.isEmpty()) {
                            serviceIp = "localhost";
                        }
                    } catch (Exception ex) {
                        logger.warning("Failed to get pod node IP, using localhost: " + ex.getMessage());
                        serviceIp = "localhost";
                    }
                }
            }

            return new ContainerCreateResult(deploymentName, exposedPorts, serviceIp);

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
     * Get the IP of the node where the pod is running
     */
    private String getPodNodeIp(String podName) {
        try {
            V1PodList podList = coreApi.listNamespacedPod(namespace)
                    .labelSelector("app=" + podName)
                    .execute();
            
            if (podList.getItems().isEmpty()) {
                return null;
            }
            
            V1Pod pod = podList.getItems().get(0);
            String nodeName = pod.getSpec().getNodeName();
            if (nodeName == null) {
                return null;
            }
            
            V1Node node = coreApi.readNode(nodeName).execute();
            if (node.getStatus() != null && node.getStatus().getAddresses() != null) {
                for (V1NodeAddress address : node.getStatus().getAddresses()) {
                    if ("ExternalIP".equals(address.getType())) {
                        return address.getAddress();
                    }
                }
                for (V1NodeAddress address : node.getStatus().getAddresses()) {
                    if ("InternalIP".equals(address.getType())) {
                        return address.getAddress();
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.warning("Failed to get pod node IP: " + e.getMessage());
            return null;
        }
    }

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

    private V1Service createLoadBalancerServiceObject(String serviceName, String appName,
                                                      Map<String, Integer> portMapping) {
        List<V1ServicePort> servicePorts = new ArrayList<>();
        int index = 1;

        for (Map.Entry<String, Integer> entry : portMapping.entrySet()) {
            String containerPortSpec = entry.getKey();
            Integer servicePort = entry.getValue();

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

    private V1Deployment createDeploymentObject(String deploymentName, String imageName,
                                                List<String> ports, Map<String, Integer> portMapping,
                                                List<VolumeBinding> volumeBindings,
                                                Map<String, String> environment, Map<String, Object> runtimeConfig) {

        Map<String, String> labels = Collections.singletonMap("app", deploymentName);

        Set<Integer> containerPortNumbers = new HashSet<>();
        List<V1ContainerPort> containerPorts = new ArrayList<>();

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

        List<V1EnvVar> envVars = new ArrayList<>();
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envVars.add(new V1EnvVar().name(entry.getKey()).value(entry.getValue()));
            }
        }

        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        List<V1Volume> volumes = new ArrayList<>();

        volumeBindings=null;
        if (volumeBindings != null && !volumeBindings.isEmpty()) {
            for (int i = 0; i < volumeBindings.size(); i++) {
                VolumeBinding binding = volumeBindings.get(i);
                String volumeName = "vol-" + i;

                volumeMounts.add(new V1VolumeMount()
                        .name(volumeName)
                        .mountPath(binding.getContainerPath())
                        .readOnly("ro".equals(binding.getMode())));

                V1HostPathVolumeSource hostPath = new V1HostPathVolumeSource()
                        .path(binding.getHostPath())
                        .type("DirectoryOrCreate");

                volumes.add(new V1Volume()
                        .name(volumeName)
                        .hostPath(hostPath));
            }
        }

        V1Container container = new V1Container()
                .name(deploymentName)
                .image(imageName)
                .ports(containerPorts)
                .env(envVars)
                .volumeMounts(volumeMounts)
                .imagePullPolicy("IfNotPresent");

        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            logger.info("Applying runtime configuration to Kubernetes container: " + runtimeConfig);
            container = applyRuntimeConfigToContainer(container, runtimeConfig);
        }

        V1PodSpec podSpec = new V1PodSpec()
                .containers(Arrays.asList(container));

        if (!volumes.isEmpty()) {
            podSpec.volumes(volumes);
        }

        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            podSpec = applyRuntimeConfigToPodSpec(podSpec, runtimeConfig);
        }

        V1PodTemplateSpec podTemplate = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(podSpec);

        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
                .replicas(1)
                .selector(new V1LabelSelector().matchLabels(labels))
                .template(podTemplate);

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
            appsApi.deleteNamespacedDeployment(containerId, namespace).execute();
            logger.info("Deployment " + containerId + " deleted successfully");
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
            appsApi.deleteNamespacedDeployment(containerId, namespace).execute();
            logger.info("Deployment " + containerId + " deleted successfully");

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

    public String waitForLoadBalancerExternalIP(String containerId, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            String externalIP = getLoadBalancerExternalIP(containerId);
            if (externalIP != null && !externalIP.isEmpty()) {
                return externalIP;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.warning("Timeout waiting for LoadBalancer External IP for container: " + containerId);
        return null;
    }

    private V1Container applyRuntimeConfigToContainer(V1Container container, Map<String, Object> runtimeConfig) {
        if (runtimeConfig == null || runtimeConfig.isEmpty()) {
            return container;
        }

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

        if (runtimeConfig.containsKey("mem_limit")) {
            Object memLimitObj = runtimeConfig.get("mem_limit");
            Long memoryBytes = parseMemoryLimit(memLimitObj);
            if (memoryBytes != null) {
                io.kubernetes.client.custom.Quantity memoryQuantity = new io.kubernetes.client.custom.Quantity(
                        String.valueOf(memoryBytes));
                limits.put("memory", memoryQuantity);
                requests.put("memory", memoryQuantity);
                logger.info("Applied memory limit: " + memoryBytes + " bytes");
            }
        }

        if (runtimeConfig.containsKey("nano_cpus")) {
            Object nanoCpusObj = runtimeConfig.get("nano_cpus");
            Long nanoCpus = parseNanoCpus(nanoCpusObj);
            if (nanoCpus != null) {
                // Convert nanocpus to millicores: 1,000,000,000 nano = 1 core = 1000m
                long milliCpus = nanoCpus / 1_000_000;
                io.kubernetes.client.custom.Quantity cpuQuantity = new io.kubernetes.client.custom.Quantity(
                        milliCpus + "m");
                limits.put("cpu", cpuQuantity);
                requests.put("cpu", cpuQuantity);
                logger.info("Applied CPU limit: " + nanoCpus + " nanocpus (" + milliCpus + " millicores)");
            }
        }

        if (runtimeConfig.containsKey("enable_gpu")) {
            Object enableGpuObj = runtimeConfig.get("enable_gpu");
            boolean enableGpu = parseBoolean(enableGpuObj);
            if (enableGpu) {
                io.kubernetes.client.custom.Quantity gpuQuantity = new io.kubernetes.client.custom.Quantity("1");
                limits.put("nvidia.com/gpu", gpuQuantity);
                requests.put("nvidia.com/gpu", gpuQuantity);
                logger.info("Applied GPU support: enabled (1 GPU requested)");
            }
        }

        if (runtimeConfig.containsKey("max_connections")) {
            Object maxConnectionsObj = runtimeConfig.get("max_connections");
            Integer maxConnections = parseInteger(maxConnectionsObj);
            if (maxConnections != null) {
                logger.info("Max connections configuration noted: " + maxConnections +
                        " (Note: Kubernetes doesn't directly support ulimits, consider using init containers or node-level configuration)");
            }
        }

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

        if (runtimeConfig.containsKey("enable_gpu")) {
            Object enableGpuObj = runtimeConfig.get("enable_gpu");
            boolean enableGpu = parseBoolean(enableGpuObj);
            if (enableGpu) {
                Map<String, String> nodeSelector = podSpec.getNodeSelector();
                if (nodeSelector == null) {
                    nodeSelector = new HashMap<>();
                }
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
            appsApi.readNamespacedDeployment(containerIdOrName, namespace).execute();
            return true;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            logger.warning("Error inspecting container " + containerIdOrName + ": " + e.getMessage());
            return false;
        }
    }
}
