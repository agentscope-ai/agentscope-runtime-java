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

package io.agentscope.maven.plugin.service;

import io.agentscope.maven.plugin.config.BuildConfig;
import io.agentscope.maven.plugin.config.K8sConfig;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.apache.maven.plugin.logging.Log;

import java.util.*;

/**
 * Service for deploying to Kubernetes
 */
public class K8sDeployService {

    private final Log log;
    private ApiClient apiClient;
    private AppsV1Api appsApi;
    private CoreV1Api coreApi;
    private static final int LOAD_BALANCER_WAIT_SECONDS = 60;
    private boolean connected = false;
    private static final int DEFAULT_CONTAINER_PORT = 8080;
    private static final int LOAD_BALANCER_PORT = 80;

    public K8sDeployService(Log log) {
        this.log = log;
    }

    /**
     * Deploy to Kubernetes following SandboxService's Kubernetes client logic
     */
    public String deploy(String imageName, BuildConfig buildConfig, K8sConfig k8sConfig) throws Exception {
        ensureConnected(k8sConfig);
        if (!connected || appsApi == null || coreApi == null) {
            throw new IllegalStateException("Kubernetes client is not connected");
        }

        String namespace = k8sConfig.getK8sNamespace();
        namespace = "default";
        String deploymentName = buildDeploymentName(buildConfig.getImageName());
        int containerPort = DEFAULT_CONTAINER_PORT;
        int servicePort = LOAD_BALANCER_PORT;

        log.info("Starting deployment to Kubernetes");
        log.info("Namespace: " + namespace);
        log.info("Deployment name: " + deploymentName);
        log.info("Image: " + imageName);

        ensureNamespace(namespace);

        Map<String, Integer> portMapping = new HashMap<>();
        portMapping.put(buildConfig.getPort()+"/tcp", 80);

        // Create or update Deployment
        V1Deployment deployment = createDeployment(deploymentName, imageName, containerPort,
                k8sConfig.getReplicas(), buildConfig.getEnvironment(), k8sConfig.getRuntimeConfig());
        V1Deployment createdDeployment = appsApi.createNamespacedDeployment(namespace, deployment).execute();
        log.info("Applying deployment..." + createdDeployment.getMetadata().getName());

        // Always create LoadBalancer service mapping 80 -> container port
        V1Service service = createLoadBalancerService(deploymentName, containerPort, servicePort);
        upsertService(namespace, deploymentName, service);

        return buildServiceUrl(namespace, deploymentName, servicePort);
    }

    private void ensureConnected(K8sConfig k8sConfig) {
        if (connected) {
            return;
        }
        try {
            String kubeconfigPath = resolveKubeconfig(k8sConfig);
            if (kubeconfigPath != null && !kubeconfigPath.isEmpty()) {
                apiClient = Config.fromConfig(kubeconfigPath);
                log.info("Initializing Kubernetes client with kubeconfig: " + kubeconfigPath);
            } else {
                apiClient = Config.defaultClient();
                log.info("Initializing Kubernetes client with default configuration");
            }
            Configuration.setDefaultApiClient(apiClient);
            appsApi = new AppsV1Api(apiClient);
            coreApi = new CoreV1Api(apiClient);
            // connection test
            coreApi.getAPIResources().execute();
            connected = true;
        } catch (Exception e) {
            log.error("Failed to initialize Kubernetes client", e);
            connected = false;
        }
    }

    private String resolveKubeconfig(K8sConfig config) {
        if (config != null && config.getKubeconfigPath() != null && !config.getKubeconfigPath().isEmpty()) {
            return config.getKubeconfigPath();
        }
        return System.getenv("KUBECONFIG");
    }

    private String buildDeploymentName(String imageName) {
        String base = imageName != null ? imageName : "agent";
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        return base.startsWith("agent-") ? base : "agent-" + base;
    }

    private void ensureNamespace(String namespace) throws ApiException {
        try {
            coreApi.readNamespace(namespace).execute();
        } catch (ApiException ex) {
            if (ex.getCode() == 404) {
                log.info("Namespace not found, creating: " + namespace);
                V1Namespace ns = new V1Namespace().metadata(new V1ObjectMeta().name(namespace));
                coreApi.createNamespace(ns).execute();
            } else {
                throw ex;
            }
        }
    }

    private V1Deployment createDeployment(String name,
                                          String image,
                                          int containerPort,
                                          int replicas,
                                          Map<String, String> environment,
                                          Map<String, String> runtimeConfig) {
        Map<String, String> labels = Collections.singletonMap("app", name);

        V1Container container = new V1Container()
                .name(name)
                .image(image)
                .imagePullPolicy("IfNotPresent")
                .ports(Collections.singletonList(new V1ContainerPort().containerPort(containerPort)));

        if (environment != null && !environment.isEmpty()) {
            List<V1EnvVar> envVars = new ArrayList<>();
            environment.forEach((k, v) -> envVars.add(new V1EnvVar().name(k).value(v)));
            container.setEnv(envVars);
        }

        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            applyRuntimeConfig(container, runtimeConfig);
        }

        V1PodSpec podSpec = new V1PodSpec().containers(Collections.singletonList(container));
        if (runtimeConfig != null && !runtimeConfig.isEmpty()) {
            applyRuntimeConfigToPodSpec(podSpec, runtimeConfig);
        }

        V1PodTemplateSpec template = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(podSpec);

        V1DeploymentSpec spec = new V1DeploymentSpec()
                .replicas(Math.max(replicas, 1))
                .selector(new V1LabelSelector().matchLabels(labels))
                .template(template);

        return new V1Deployment()
                .metadata(new V1ObjectMeta().name(name).labels(labels))
                .spec(spec);
    }

    private void upsertDeployment(String namespace, String name, V1Deployment deployment) throws Exception {
        try {
            appsApi.createNamespacedDeployment(namespace, deployment).execute();
            log.info("Deployment created: " + name);
        } catch (ApiException ex) {
            if (ex.getCode() == 409) {
                log.info("Deployment exists, updating: " + name);
                appsApi.replaceNamespacedDeployment(name, namespace, deployment).execute();
                log.info("Deployment updated: " + name);
            } else {
                throw new Exception("Failed to apply deployment", ex);
            }
        }
    }

    private V1Service createLoadBalancerService(String name, int targetPort, int servicePort) {
        Map<String, String> selector = Collections.singletonMap("app", name);
        V1ServicePort port = new V1ServicePort()
                .name("http")
                .protocol("TCP")
                .port(servicePort)
                .targetPort(new IntOrString(targetPort));

        V1ServiceSpec spec = new V1ServiceSpec()
                .type("LoadBalancer")
                .selector(selector)
                .ports(Collections.singletonList(port));

        return new V1Service()
                .metadata(new V1ObjectMeta().name(name).labels(selector))
                .spec(spec);
    }

    private void upsertService(String namespace, String name, V1Service service) throws Exception {
        try {
            coreApi.createNamespacedService(namespace, service).execute();
            log.info("Service created: " + name);
        } catch (ApiException ex) {
            if (ex.getCode() == 409) {
                log.info("Service exists, updating: " + name);
                coreApi.replaceNamespacedService(name, namespace, service).execute();
                log.info("Service updated: " + name);
            } else {
                throw new Exception("Failed to apply service", ex);
            }
        }
    }

    private String buildServiceUrl(String namespace, String serviceName, int port) {
        try {
            String lbIp = waitForLoadBalancer(namespace, serviceName, LOAD_BALANCER_WAIT_SECONDS);
            if (lbIp != null && !lbIp.isEmpty()) {
                return "http://" + lbIp + ":" + port;
            }
        } catch (Exception e) {
            log.warn("LoadBalancer IP not ready: " + e.getMessage());
        }

        try {
            V1Service service = coreApi.readNamespacedService(serviceName, namespace).execute();
            if (service.getSpec() != null && service.getSpec().getClusterIP() != null) {
                return "http://" + service.getSpec().getClusterIP() + ":" + port;
            }
        } catch (Exception e) {
            log.warn("Failed to read service info: " + e.getMessage());
        }

        return String.format("http://%s.%s.svc.cluster.local:%d", serviceName, namespace, port);
    }

    private String waitForLoadBalancer(String namespace, String serviceName, int timeoutSeconds)
            throws ApiException, InterruptedException {
        long start = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - start < timeoutMs) {
            V1Service service = coreApi.readNamespacedService(serviceName, namespace).execute();
            if (service.getStatus() != null
                    && service.getStatus().getLoadBalancer() != null
                    && service.getStatus().getLoadBalancer().getIngress() != null
                    && !service.getStatus().getLoadBalancer().getIngress().isEmpty()) {

                V1LoadBalancerIngress ingress = service.getStatus().getLoadBalancer().getIngress().get(0);
                if (ingress.getIp() != null && !ingress.getIp().isEmpty()) {
                    return ingress.getIp();
                }
                if (ingress.getHostname() != null && !ingress.getHostname().isEmpty()) {
                    return ingress.getHostname();
                }
            }
            Thread.sleep(2000);
        }
        return null;
    }

    private void applyRuntimeConfig(V1Container container, Map<String, String> runtimeConfig) {
        V1ResourceRequirements resources = container.getResources();
        if (resources == null) {
            resources = new V1ResourceRequirements();
        }

        Map<String, Quantity> limits = resources.getLimits();
        Map<String, Quantity> requests = resources.getRequests();
        if (limits == null) {
            limits = new HashMap<>();
        }
        if (requests == null) {
            requests = new HashMap<>();
        }

        String memLimit = runtimeConfig.get("mem_limit");
        if (memLimit != null) {
            Long bytes = parseMemoryLimit(memLimit);
            if (bytes != null) {
                Quantity quantity = new Quantity(String.valueOf(bytes));
                limits.put("memory", quantity);
                requests.put("memory", quantity);
            }
        }

        String nanoCpus = runtimeConfig.get("nano_cpus");
        if (nanoCpus != null) {
            Long nanos = parseNanoCpus(nanoCpus);
            if (nanos != null) {
                long milli = nanos / 1_000_000;
                Quantity quantity = new Quantity(milli + "m");
                limits.put("cpu", quantity);
                requests.put("cpu", quantity);
            }
        }

        if (parseBoolean(runtimeConfig.get("enable_gpu"))) {
            Quantity gpuQuantity = new Quantity("1");
            limits.put("nvidia.com/gpu", gpuQuantity);
            requests.put("nvidia.com/gpu", gpuQuantity);
        }

        resources.setLimits(limits);
        resources.setRequests(requests);
        container.setResources(resources);
    }

    private void applyRuntimeConfigToPodSpec(V1PodSpec podSpec, Map<String, String> runtimeConfig) {
        if (parseBoolean(runtimeConfig.get("enable_gpu"))) {
            Map<String, String> selector = podSpec.getNodeSelector();
            if (selector == null) {
                selector = new HashMap<>();
            }
            selector.putIfAbsent("accelerator", "nvidia-gpu");
            podSpec.setNodeSelector(selector);
        }
    }

    private Long parseMemoryLimit(String value) {
        if (value == null) {
            return null;
        }
        try {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            String numberPart = normalized.replaceAll("[^0-9.]", "");
            String unitPart = normalized.replaceAll("[0-9.]", "");
            double numeric = Double.parseDouble(numberPart);

            return switch (unitPart) {
                case "k", "kb" -> (long) (numeric * 1024);
                case "m", "mb" -> (long) (numeric * 1024 * 1024);
                case "g", "gb" -> (long) (numeric * 1024 * 1024 * 1024);
                case "t", "tb" -> (long) (numeric * 1024 * 1024 * 1024 * 1024);
                case "" -> (long) numeric;
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Failed to parse memory limit: " + value);
            return null;
        }
    }

    private Long parseNanoCpus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse nano CPUs: " + value);
            return null;
        }
    }

    private boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }
}

