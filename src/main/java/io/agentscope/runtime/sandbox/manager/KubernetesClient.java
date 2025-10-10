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

import io.agentscope.runtime.sandbox.manager.model.VolumeBinding;
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
 * Kubernetes容器管理客户端实现
 * 基于Kubernetes Java客户端实现容器管理功能
 */
public class KubernetesClient extends BaseClient {
    
    private static final Logger logger = Logger.getLogger(KubernetesClient.class.getName());
    private static final String DEFAULT_NAMESPACE = "default";
    
    private ApiClient apiClient;
    private CoreV1Api coreApi;
    private AppsV1Api appsApi;
    private boolean connected = false;
    private String kubeconfigPath = null;
    
    /**
     * 默认构造函数
     */
    public KubernetesClient() {
        // 使用默认配置
    }
    
    /**
     * 构造函数，指定 kubeconfig 文件路径
     * 
     * @param kubeconfigPath kubeconfig 文件路径
     */
    public KubernetesClient(String kubeconfigPath) {
        this.kubeconfigPath = kubeconfigPath;
    }
    
    @Override
    public boolean connect() {
        try {
            // 根据是否指定了 kubeconfig 文件路径来选择连接方式
            if (kubeconfigPath != null && !kubeconfigPath.trim().isEmpty()) {
                // 从指定的 kubeconfig 文件加载配置
                File kubeconfigFile = new File(kubeconfigPath);
                if (!kubeconfigFile.exists()) {
                    throw new RuntimeException("Kubeconfig file not found: " + kubeconfigPath);
                }
                logger.info("Loading Kubernetes configuration from file: " + kubeconfigPath);
                this.apiClient = Config.fromConfig(kubeconfigPath);
            } else {
                // 使用默认配置连接Kubernetes集群
                logger.info("Using default Kubernetes configuration");
                this.apiClient = Config.defaultClient();
            }
            Configuration.setDefaultApiClient(this.apiClient);
            
            // 初始化API客户端
            this.coreApi = new CoreV1Api();
            this.appsApi = new AppsV1Api();
            
            try {
                this.coreApi.getAPIResources().execute();
                logger.info("Successfully connected to Kubernetes cluster");
                this.connected = true;
                return true;
            } catch (Exception e) {
                logger.warning("API resources check failed, trying alternative connection test: " + e.getMessage());
                
                // 使用kubectl命令进行连接测试
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
     * 使用指定的 kubeconfig 文件路径连接
     * 
     * @param kubeconfigPath kubeconfig 文件路径
     * @return 连接是否成功
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
     * 获取当前使用的 kubeconfig 文件路径
     * 
     * @return kubeconfig 文件路径，如果使用默认配置则返回 null
     */
    public String getKubeconfigPath() {
        return kubeconfigPath;
    }

    @Override
    public String createContainer(String containerName, String imageName,
                                  List<String> ports, Map<String, Integer> portMapping,
                                  List<VolumeBinding> volumeBindings,
                                  Map<String, String> environment, String runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        String deploymentName = containerName;
        String serviceName = containerName + "-svc"; // Service 名称

        try {
            // Step 1: 创建 Deployment
            V1Deployment deployment = createDeploymentObject(deploymentName, imageName,
                    ports, portMapping, volumeBindings, environment, runtimeConfig);
            V1Deployment createdDeployment = appsApi.createNamespacedDeployment(DEFAULT_NAMESPACE, deployment).execute();
            logger.info("Deployment created: " + createdDeployment.getMetadata().getName());

            // Step 2: 如果有 portMapping，创建 LoadBalancer Service
            if (portMapping != null && !portMapping.isEmpty()) {
                V1Service service = createLoadBalancerServiceObject(serviceName, deploymentName, portMapping);
                V1Service createdService = coreApi.createNamespacedService(DEFAULT_NAMESPACE, service).execute();
                logger.info("LoadBalancer Service created: " + createdService.getMetadata().getName() +
                        ", ports: " + createdService.getSpec().getPorts().stream()
                        .map(p -> p.getPort() + "->" + p.getTargetPort())
                        .toList());
            }

            return deploymentName; // 返回 Deployment 名称作为容器 ID

        } catch (ApiException e) {
            logger.severe("Failed to create container (Deployment/Service): " + e.getMessage());
            try {
                appsApi.deleteNamespacedDeployment(deploymentName, DEFAULT_NAMESPACE).execute();
                coreApi.deleteNamespacedService(serviceName, DEFAULT_NAMESPACE).execute();
                logger.info("Rolled back resources for: " + containerName);
            } catch (Exception cleanupEx) {
                logger.warning("Failed to rollback after createContainer error: " + cleanupEx.getMessage());
            }
            throw new RuntimeException("Failed to create container", e);
        }
    }
    
    
    /**
     * 创建Deployment（更高级的控制器）
     */
    public String createDeployment(String deploymentName, String imageName,
                                  List<String> ports, Map<String, Integer> portMapping,
                                  List<VolumeBinding> volumeBindings,
                                  Map<String, String> environment, String runtimeConfig) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        
        try {
            V1Deployment deployment = createDeploymentObject(deploymentName, imageName, 
                                                          ports, portMapping, volumeBindings, 
                                                          environment, runtimeConfig);
            
            V1Deployment createdDeployment = appsApi.createNamespacedDeployment(DEFAULT_NAMESPACE, deployment).execute();
            logger.info("Deployment created successfully: " + createdDeployment.getMetadata().getName());
            return createdDeployment.getMetadata().getName();
            
        } catch (ApiException e) {
            logger.severe("Failed to create deployment: " + e.getMessage());
            throw new RuntimeException("Failed to create deployment", e);
        }
    }

    /**
     * 创建 LoadBalancer 类型的 Service 对象，用于暴露容器端口到外部
     */
    private V1Service createLoadBalancerServiceObject(String serviceName, String appName, Map<String, Integer> portMapping) {
        List<V1ServicePort> servicePorts = new ArrayList<>();
        int index = 1;

        for (Map.Entry<String, Integer> entry : portMapping.entrySet()) {
            String containerPortSpec = entry.getKey(); // e.g., "80/tcp"
            Integer servicePort = entry.getValue();    // e.g., 80

            String[] parts = containerPortSpec.split("/");
            int containerPort = Integer.parseInt(parts[0]);
            String protocol = (parts.length > 1) ? parts[1].toUpperCase() : "TCP";

            if (!"TCP".equals(protocol) && !"UDP".equals(protocol)) {
                protocol = "TCP";
            }

            V1ServicePort servicePortObj = new V1ServicePort()
                    .name("port-" + index++)
                    .port(servicePort)                         // Service 端口（LoadBalancer使用80端口）
                    .targetPort(new IntOrString(containerPort)) // 转发到 Pod 的容器端口
                    .protocol(protocol);

            servicePorts.add(servicePortObj);
        }

        // Service 选择器需匹配 Deployment 的 Pod 标签
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
     * 创建 Deployment 对象
     */
    private V1Deployment createDeploymentObject(String deploymentName, String imageName,
                                                List<String> ports, Map<String, Integer> portMapping,
                                                List<VolumeBinding> volumeBindings,
                                                Map<String, String> environment, String runtimeConfig) {

        // 标签用于选择器和关联资源
        Map<String, String> labels = Collections.singletonMap("app", deploymentName);

        // 收集所有需要暴露的容器端口
        Set<Integer> containerPortNumbers = new HashSet<>();
        List<V1ContainerPort> containerPorts = new ArrayList<>();

        // 1. 从 portMapping 中提取容器端口（值为主机端口，但我们只关心容器端口）
        if (portMapping != null && !portMapping.isEmpty()) {
            for (String containerPortStr : portMapping.keySet()) {
                String[] parts = containerPortStr.split("/");
                int portNum = Integer.parseInt(parts[0]);
                String protocol = (parts.length > 1) ? parts[1].toUpperCase() : "TCP";
                if (containerPortNumbers.add(portNum)) { // add 返回 true 表示未重复
                    V1ContainerPort port = new V1ContainerPort()
                            .containerPort(portNum)
                            .protocol(protocol);
                    containerPorts.add(port);
                }
            }
        }

        // 2. 从 ports 列表中补充其他端口
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

        // 环境变量
        List<V1EnvVar> envVars = new ArrayList<>();
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envVars.add(new V1EnvVar().name(entry.getKey()).value(entry.getValue()));
            }
        }

        // 卷挂载
        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        List<V1Volume> volumes = new ArrayList<>();
        volumeBindings=null;
        if (volumeBindings != null) {
            for (int i = 0; i < volumeBindings.size(); i++) {
                VolumeBinding binding = volumeBindings.get(i);
                String volumeName = "vol-" + i;

                volumeMounts.add(new V1VolumeMount()
                        .name(volumeName)
                        .mountPath(binding.getContainerPath())
                        .readOnly("ro".equals(binding.getMode())));

                // 使用 hostPath（注意：生产环境建议改用 PVC）
                V1HostPathVolumeSource hostPath = new V1HostPathVolumeSource()
                        .path(binding.getHostPath())
                        .type("DirectoryOrCreate");

                volumes.add(new V1Volume()
                        .name(volumeName)
                        .hostPath(hostPath));
            }
        }

        // 容器定义
        V1Container container = new V1Container()
                .name(deploymentName)
                .image(imageName)
                .ports(containerPorts)
                .env(envVars)
                .volumeMounts(volumeMounts)
                .imagePullPolicy("IfNotPresent");

        // Pod 规格
        V1PodSpec podSpec = new V1PodSpec()
                .containers(Arrays.asList(container));

        if (!volumes.isEmpty()) {
            podSpec.volumes(volumes);
        }

        // 注意：不再自动启用 hostNetwork！
        // 如果确实需要 hostNetwork（如性能敏感场景），应通过 runtimeConfig 显式开启
        // if (Boolean.parseBoolean(runtimeConfig)) { podSpec.hostNetwork(true); }

        // Pod 模板
        V1PodTemplateSpec podTemplate = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(podSpec);

        // Deployment 规格
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
                .replicas(1)
                .selector(new V1LabelSelector().matchLabels(labels))
                .template(podTemplate);

        // 最终 Deployment 对象
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
            V1Deployment deployment = appsApi.readNamespacedDeployment(containerId, DEFAULT_NAMESPACE).execute();
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
            // 删除Deployment
            appsApi.deleteNamespacedDeployment(containerId, DEFAULT_NAMESPACE).execute();
            logger.info("Deployment " + containerId + " deleted successfully");
            
            // 同时删除对应的Service
            deleteServiceIfExists(containerId);
            
        } catch (ApiException e) {
            logger.severe("Failed to stop container: " + e.getMessage());
            throw new RuntimeException("Failed to stop container", e);
        }
    }
    
    @Override
    public void removeContainer(String containerId) {
        // 在Kubernetes中，删除Deployment就是移除容器
        // 直接调用stopContainer，因为stopAndRemoveContainer会先调用stopContainer
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        
        try {
            // 删除Deployment
            appsApi.deleteNamespacedDeployment(containerId, DEFAULT_NAMESPACE).execute();
            logger.info("Deployment " + containerId + " deleted successfully");
            
            // 同时删除对应的Service
            deleteServiceIfExists(containerId);
            
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // Deployment已经不存在，这是正常的
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
            // 获取Deployment状态
            V1Deployment deployment = appsApi.readNamespacedDeployment(containerId, DEFAULT_NAMESPACE).execute();
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
     * 获取所有Pod列表
     */
    public List<V1Pod> listPods() {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        
        try {
            V1PodList podList = coreApi.listNamespacedPod(DEFAULT_NAMESPACE).execute();
            return podList.getItems();
        } catch (ApiException e) {
            logger.severe("Failed to list pods: " + e.getMessage());
            throw new RuntimeException("Failed to list pods", e);
        } catch (Exception e) {
            // 处理JSON解析错误等兼容性问题
            logger.warning("Error listing pods (possibly due to version compatibility): " + e.getMessage());
            return new ArrayList<>(); // 返回空列表而不是抛出异常
        }
    }
    
    /**
     * 获取所有Deployment列表
     */
    public List<V1Deployment> listDeployments() {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        
        try {
            V1DeploymentList deploymentList = appsApi.listNamespacedDeployment(DEFAULT_NAMESPACE).execute();
            return deploymentList.getItems();
        } catch (ApiException e) {
            logger.severe("Failed to list deployments: " + e.getMessage());
            throw new RuntimeException("Failed to list deployments", e);
        }
    }
    
    /**
     * 获取LoadBalancer Service的External IP
     * 
     * @param containerId 容器ID（Deployment名称）
     * @return External IP地址，如果未分配则返回null
     */
    public String getLoadBalancerExternalIP(String containerId) {
        if (!isConnected()) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }
        
        String serviceName = containerId + "-svc";
        
        try {
            V1Service service = coreApi.readNamespacedService(serviceName, DEFAULT_NAMESPACE).execute();
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
     * 等待LoadBalancer Service的External IP分配
     * 
     * @param containerId 容器ID（Deployment名称）
     * @param timeoutSeconds 超时时间（秒）
     * @return External IP地址，如果超时则返回null
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
                Thread.sleep(2000); // 等待2秒后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.warning("Timeout waiting for LoadBalancer External IP for container: " + containerId);
        return null;
    }

    /**
     * 删除与容器关联的Service（如果存在）
     * 
     * @param containerId 容器ID（Deployment名称）
     */
    private void deleteServiceIfExists(String containerId) {
        String serviceName = containerId + "-svc";
        
        try {
            // 尝试删除Service
            coreApi.deleteNamespacedService(serviceName, DEFAULT_NAMESPACE).execute();
            logger.info("Service " + serviceName + " deleted successfully");
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // Service不存在，这是正常的
                logger.info("Service " + serviceName + " does not exist, skipping deletion");
            } else {
                // 其他错误，记录警告但不抛出异常，避免影响主流程
                logger.warning("Failed to delete service " + serviceName + ": " + e.getMessage());
            }
        } catch (Exception e) {
            // 处理其他可能的异常
            logger.warning("Unexpected error while deleting service " + serviceName + ": " + e.getMessage());
        }
    }
}
