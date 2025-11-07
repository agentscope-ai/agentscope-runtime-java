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
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.*;

/**
 * Service for deploying to Kubernetes
 */
public class K8sDeployService {

    private final Log log;
    private ApiClient apiClient;
    private AppsV1Api appsApi;
    private CoreV1Api coreApi;

    public K8sDeployService(Log log) {
        this.log = log;
        try {
            initializeK8sClient();
        } catch (Exception e) {
            log.warn("Failed to initialize Kubernetes client: " + e.getMessage());
            log.warn("Make sure kubeconfig is properly configured");
        }
    }

    private void initializeK8sClient() throws Exception {
        if (apiClient == null) {
            String kubeconfigPath = System.getenv("KUBECONFIG");
            if (kubeconfigPath != null && !kubeconfigPath.isEmpty()) {
                apiClient = Config.fromConfig(kubeconfigPath);
            } else {
                apiClient = Config.defaultClient();
            }
            Configuration.setDefaultApiClient(apiClient);
            appsApi = new AppsV1Api(apiClient);
            coreApi = new CoreV1Api(apiClient);
        }
    }
    
    /**
     * Initialize with custom kubeconfig path
     */
    public void initializeK8sClient(String kubeconfigPath) throws Exception {
        if (apiClient == null) {
            if (kubeconfigPath != null && !kubeconfigPath.isEmpty()) {
                apiClient = Config.fromConfig(kubeconfigPath);
            } else {
                apiClient = Config.defaultClient();
            }
            Configuration.setDefaultApiClient(apiClient);
            appsApi = new AppsV1Api(apiClient);
            coreApi = new CoreV1Api(apiClient);
        }
    }

    /**
     * Deploy to Kubernetes
     */
    public String deploy(String imageName, BuildConfig buildConfig, K8sConfig k8sConfig) throws Exception {
        if (appsApi == null || coreApi == null) {
            throw new IllegalStateException("Kubernetes client is not initialized. Please check kubeconfig configuration.");
        }

        String namespace = k8sConfig.getK8sNamespace();
        String deploymentName = "agent-" + buildConfig.getImageName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        int port = buildConfig.getPort();
        int replicas = k8sConfig.getReplicas();

        log.info("Deploying to Kubernetes namespace: " + namespace);
        log.info("Deployment name: " + deploymentName);
        log.info("Image: " + imageName);
        log.info("Replicas: " + replicas);

        // Ensure namespace exists
        ensureNamespace(namespace);

        // Create or update deployment
        V1Deployment deployment = createDeployment(deploymentName, imageName, port, replicas, 
                buildConfig.getEnvironment(), k8sConfig.getRuntimeConfig());
        
        try {
            appsApi.createNamespacedDeployment(namespace, deployment).execute();
            log.info("Deployment created: " + deploymentName);
        } catch (ApiException e) {
            if (e.getCode() == 409) {
                // Deployment already exists, update it
                log.info("Deployment exists, updating...");
                appsApi.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();
                log.info("Deployment updated: " + deploymentName);
            } else {
                throw new Exception("Failed to create deployment", e);
            }
        }

        // Create service
        V1Service service = createService(deploymentName, port);
        try {
            coreApi.createNamespacedService(namespace, service).execute();
            log.info("Service created: " + deploymentName);
        } catch (ApiException e) {
            if (e.getCode() == 409) {
                // Service already exists, update it
                log.info("Service exists, updating...");
                coreApi.replaceNamespacedService(deploymentName, namespace, service).execute();
                log.info("Service updated: " + deploymentName);
            } else {
                throw new Exception("Failed to create service", e);
            }
        }

        // Get service URL
        String serviceUrl = getServiceUrl(namespace, deploymentName, port);
        return serviceUrl;
    }

    private void ensureNamespace(String namespace) throws ApiException {
        try {
            coreApi.readNamespace(namespace).execute();
            log.debug("Namespace exists: " + namespace);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Creating namespace: " + namespace);
                V1Namespace ns = new V1Namespace();
                V1ObjectMeta metadata = new V1ObjectMeta();
                metadata.setName(namespace);
                ns.setMetadata(metadata);
                coreApi.createNamespace(ns).execute();
                log.info("Namespace created: " + namespace);
            } else {
                throw e;
            }
        }
    }

    private V1Deployment createDeployment(String name, String image, int port, int replicas,
                                         Map<String, String> environment, Map<String, String> runtimeConfig) {
        V1Deployment deployment = new V1Deployment();
        
        // Metadata
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(name);
        metadata.setLabels(Collections.singletonMap("app", name));
        deployment.setMetadata(metadata);

        // Spec
        V1DeploymentSpec spec = new V1DeploymentSpec();
        spec.setReplicas(replicas);

        // Selector
        V1LabelSelector selector = new V1LabelSelector();
        selector.setMatchLabels(Collections.singletonMap("app", name));
        spec.setSelector(selector);

        // Template
        V1PodTemplateSpec template = new V1PodTemplateSpec();
        V1ObjectMeta podMetadata = new V1ObjectMeta();
        podMetadata.setLabels(Collections.singletonMap("app", name));
        template.setMetadata(podMetadata);

        // Pod spec
        V1PodSpec podSpec = new V1PodSpec();
        
        // Container
        V1Container container = new V1Container();
        container.setName(name);
        container.setImage(image);
        container.setImagePullPolicy("IfNotPresent");

        // Ports
        V1ContainerPort containerPort = new V1ContainerPort();
        containerPort.setContainerPort(port);
        container.setPorts(Collections.singletonList(containerPort));

        // Environment variables
        if (environment != null && !environment.isEmpty()) {
            List<V1EnvVar> envVars = new ArrayList<>();
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                V1EnvVar envVar = new V1EnvVar();
                envVar.setName(entry.getKey());
                envVar.setValue(entry.getValue());
                envVars.add(envVar);
            }
            container.setEnv(envVars);
        }

        podSpec.setContainers(Collections.singletonList(container));
        template.setSpec(podSpec);
        spec.setTemplate(template);
        deployment.setSpec(spec);

        return deployment;
    }

    private V1Service createService(String name, int port) {
        V1Service service = new V1Service();
        
        // Metadata
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(name);
        metadata.setLabels(Collections.singletonMap("app", name));
        service.setMetadata(metadata);

        // Spec
        V1ServiceSpec spec = new V1ServiceSpec();
        spec.setType("ClusterIP");
        spec.setSelector(Collections.singletonMap("app", name));

        // Ports
        V1ServicePort servicePort = new V1ServicePort();
        servicePort.setPort(port);
        servicePort.setTargetPort(new io.kubernetes.client.custom.IntOrString(port));
        servicePort.setProtocol("TCP");
        spec.setPorts(Collections.singletonList(servicePort));

        service.setSpec(spec);
        return service;
    }

    private String getServiceUrl(String namespace, String serviceName, int port) {
        // In a real scenario, you might want to get the actual service IP
        // For now, return a placeholder URL
        return String.format("http://%s.%s.svc.cluster.local:%d", serviceName, namespace, port);
    }
}

