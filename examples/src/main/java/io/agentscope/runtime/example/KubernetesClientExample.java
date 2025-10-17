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
package io.agentscope.runtime.example;

import io.agentscope.runtime.sandbox.manager.client.KubernetesClient;
import io.agentscope.runtime.sandbox.manager.model.VolumeBinding;

import java.util.*;

/**
 * KubernetesClient usage example
 * Demonstrates how to use KubernetesClient to create and manage Pods/Deployments
 */
public class KubernetesClientExample {
    
    public static void main(String[] args) {
        if (System.getenv("KUBECONFIG_PATH") == null || System.getenv("KUBECONFIG_PATH").isEmpty()) {
            System.err.println("Please set the KUBECONFIG_PATH environment variable to point to your kubeconfig file");
            return;
        }
        KubernetesClient k8sClient = new KubernetesClient(System.getenv("KUBECONFIG_PATH"));
        
        try {
            // Connect to Kubernetes cluster
            System.out.println("Connecting to Kubernetes cluster...");
            boolean connected = k8sClient.connect();
            if (!connected) {
                System.err.println("Unable to connect to Kubernetes cluster, please ensure kubectl is configured correctly");
                return;
            }
            System.out.println("Successfully connected to Kubernetes cluster");

            // Create Pod example
            createPodExample(k8sClient);

            // Create Deployment example
            createDeploymentExample(k8sClient);
            
            // List all Pods
            listPodsExample(k8sClient);
            
            // List all Deployments
            listDeploymentsExample(k8sClient);
            
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create Deployment example (via createContainer method)
     */
    private static void createPodExample(KubernetesClient k8sClient) {
        System.out.println("\n=== Create Deployment Example (via createContainer method) ===");
        
        try {
            String containerName = "test-deployment-" + System.currentTimeMillis();
            String imageName = "nginx:latest";
            
            // Port configuration
            List<String> ports = Arrays.asList("80/TCP");
            Map<String, Integer> portMapping = new HashMap<>();
            portMapping.put("80", 8080);
            
            // Volume mount configuration (simplified, no volume mounts)
            List<VolumeBinding> volumeBindings = new ArrayList<>();
            
            // Environment variables
            Map<String, String> environment = new HashMap<>();
            environment.put("ENV_VAR_1", "value1");
            environment.put("ENV_VAR_2", "value2");
            
            String runtimeConfig = "runc";
            
            // Create Deployment (via createContainer method)
            String deploymentId = k8sClient.createContainer(containerName, imageName, 
                                                           ports, portMapping, volumeBindings, 
                                                           environment, runtimeConfig);
            
            System.out.println("Deployment created successfully, ID: " + deploymentId);
            
            // Check Deployment status
            Thread.sleep(3000); // Wait for Deployment to start
            String status = k8sClient.getContainerStatus(deploymentId);
            System.out.println("Deployment status: " + status);
            
            // Clean up Deployment
            Thread.sleep(5000); // Let Deployment run for a while
            k8sClient.stopAndRemoveContainer(deploymentId);
            System.out.println("Deployment deleted");
            
        } catch (Exception e) {
            System.err.println("Error occurred while creating Deployment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create Deployment example
     */
    private static void createDeploymentExample(KubernetesClient k8sClient) {
        System.out.println("\n=== Create Deployment Example ===");
        
        try {
            String deploymentName = "test-deployment-" + System.currentTimeMillis();
            String imageName = "nginx:latest";
            
            // Port configuration
            List<String> ports = Arrays.asList("80/TCP");
            Map<String, Integer> portMapping = new HashMap<>();
            portMapping.put("80", 8081);
            
            // Volume mount configuration (simplified, no volume mounts)
            List<VolumeBinding> volumeBindings = new ArrayList<>();
            
            // Environment variables
            Map<String, String> environment = new HashMap<>();
            environment.put("ENV_VAR_1", "value1");
            environment.put("ENV_VAR_2", "value2");
            
            String runtimeConfig = "runc";
            
            // Create Deployment
            String deploymentId = k8sClient.createDeployment(deploymentName, imageName, 
                                                          ports, portMapping, volumeBindings, 
                                                          environment, runtimeConfig);
            
            System.out.println("Deployment created successfully, ID: " + deploymentId);
            
            // Check Deployment status
            Thread.sleep(3000); // Wait for Deployment to start
            String status = k8sClient.getContainerStatus(deploymentId);
            System.out.println("Deployment status: " + status);
            
            // Clean up Deployment
            Thread.sleep(5000); // Let Deployment run for a while
            k8sClient.stopAndRemoveContainer(deploymentId);
            System.out.println("Deployment deleted");
            
        } catch (Exception e) {
            System.err.println("Error occurred while creating Deployment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * List all Pods example
     */
    private static void listPodsExample(KubernetesClient k8sClient) {
        System.out.println("\n=== List All Pods ===");
        
        try {
            List<io.kubernetes.client.openapi.models.V1Pod> pods = k8sClient.listPods();
            System.out.println("Found " + pods.size() + " Pods:");
            
            for (io.kubernetes.client.openapi.models.V1Pod pod : pods) {
                String name = pod.getMetadata().getName();
                String status = pod.getStatus().getPhase();
                System.out.println("- Pod: " + name + " (Status: " + status + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error occurred while listing Pods: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * List all Deployments example
     */
    private static void listDeploymentsExample(KubernetesClient k8sClient) {
        System.out.println("\n=== List All Deployments ===");
        
        try {
            List<io.kubernetes.client.openapi.models.V1Deployment> deployments = k8sClient.listDeployments();
            System.out.println("Found " + deployments.size() + " Deployments:");
            
            for (io.kubernetes.client.openapi.models.V1Deployment deployment : deployments) {
                String name = deployment.getMetadata().getName();
                Integer replicas = deployment.getSpec().getReplicas();
                System.out.println("- Deployment: " + name + " (Replicas: " + replicas + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error occurred while listing Deployments: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
