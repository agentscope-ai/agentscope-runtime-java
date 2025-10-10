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

import io.agentscope.runtime.sandbox.manager.KubernetesClient;
import io.agentscope.runtime.sandbox.manager.model.VolumeBinding;

import java.util.*;

/**
 * KubernetesClient使用示例
 * 演示如何使用KubernetesClient创建和管理Pod/Deployment
 */
public class KubernetesClientExample {
    
    public static void main(String[] args) {
        KubernetesClient k8sClient = new KubernetesClient("/Users/xht/Downloads/agentscope-runtime-java/kubeconfig.txt");
        
        try {
            // 连接到Kubernetes集群
            System.out.println("正在连接到Kubernetes集群...");
            boolean connected = k8sClient.connect();
            if (!connected) {
                System.err.println("无法连接到Kubernetes集群，请确保kubectl配置正确");
                return;
            }
            System.out.println("成功连接到Kubernetes集群");
            
            // 创建Pod示例
            createPodExample(k8sClient);
            
            // 创建Deployment示例
            createDeploymentExample(k8sClient);
            
            // 列出所有Pod
            listPodsExample(k8sClient);
            
            // 列出所有Deployment
            listDeploymentsExample(k8sClient);
            
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建Deployment示例（通过createContainer方法）
     */
    private static void createPodExample(KubernetesClient k8sClient) {
        System.out.println("\n=== 创建Deployment示例（通过createContainer方法） ===");
        
        try {
            String containerName = "test-deployment-" + System.currentTimeMillis();
            String imageName = "nginx:latest";
            
            // 端口配置
            List<String> ports = Arrays.asList("80/TCP");
            Map<String, Integer> portMapping = new HashMap<>();
            portMapping.put("80", 8080);
            
            // 卷挂载配置（简化，不使用卷挂载）
            List<VolumeBinding> volumeBindings = new ArrayList<>();
            
            // 环境变量
            Map<String, String> environment = new HashMap<>();
            environment.put("ENV_VAR_1", "value1");
            environment.put("ENV_VAR_2", "value2");
            
            String runtimeConfig = "runc";
            
            // 创建Deployment（通过createContainer方法）
            String deploymentId = k8sClient.createContainer(containerName, imageName, 
                                                           ports, portMapping, volumeBindings, 
                                                           environment, runtimeConfig);
            
            System.out.println("Deployment创建成功，ID: " + deploymentId);
            
            // 检查Deployment状态
            Thread.sleep(3000); // 等待Deployment启动
            String status = k8sClient.getContainerStatus(deploymentId);
            System.out.println("Deployment状态: " + status);
            
            // 清理Deployment
            Thread.sleep(5000); // 让Deployment运行一段时间
            k8sClient.stopAndRemoveContainer(deploymentId);
            System.out.println("Deployment已删除");
            
        } catch (Exception e) {
            System.err.println("创建Deployment时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建Deployment示例
     */
    private static void createDeploymentExample(KubernetesClient k8sClient) {
        System.out.println("\n=== 创建Deployment示例 ===");
        
        try {
            String deploymentName = "test-deployment-" + System.currentTimeMillis();
            String imageName = "nginx:latest";
            
            // 端口配置
            List<String> ports = Arrays.asList("80/TCP");
            Map<String, Integer> portMapping = new HashMap<>();
            portMapping.put("80", 8081);
            
            // 卷挂载配置（简化，不使用卷挂载）
            List<VolumeBinding> volumeBindings = new ArrayList<>();
            
            // 环境变量
            Map<String, String> environment = new HashMap<>();
            environment.put("ENV_VAR_1", "value1");
            environment.put("ENV_VAR_2", "value2");
            
            String runtimeConfig = "runc";
            
            // 创建Deployment
            String deploymentId = k8sClient.createDeployment(deploymentName, imageName, 
                                                          ports, portMapping, volumeBindings, 
                                                          environment, runtimeConfig);
            
            System.out.println("Deployment创建成功，ID: " + deploymentId);
            
            // 检查Deployment状态
            Thread.sleep(3000); // 等待Deployment启动
            String status = k8sClient.getContainerStatus(deploymentId);
            System.out.println("Deployment状态: " + status);
            
            // 清理Deployment
            Thread.sleep(5000); // 让Deployment运行一段时间
            k8sClient.stopAndRemoveContainer(deploymentId);
            System.out.println("Deployment已删除");
            
        } catch (Exception e) {
            System.err.println("创建Deployment时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 列出所有Pod示例
     */
    private static void listPodsExample(KubernetesClient k8sClient) {
        System.out.println("\n=== 列出所有Pod ===");
        
        try {
            List<io.kubernetes.client.openapi.models.V1Pod> pods = k8sClient.listPods();
            System.out.println("找到 " + pods.size() + " 个Pod:");
            
            for (io.kubernetes.client.openapi.models.V1Pod pod : pods) {
                String name = pod.getMetadata().getName();
                String status = pod.getStatus().getPhase();
                System.out.println("- Pod: " + name + " (状态: " + status + ")");
            }
            
        } catch (Exception e) {
            System.err.println("列出Pod时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 列出所有Deployment示例
     */
    private static void listDeploymentsExample(KubernetesClient k8sClient) {
        System.out.println("\n=== 列出所有Deployment ===");
        
        try {
            List<io.kubernetes.client.openapi.models.V1Deployment> deployments = k8sClient.listDeployments();
            System.out.println("找到 " + deployments.size() + " 个Deployment:");
            
            for (io.kubernetes.client.openapi.models.V1Deployment deployment : deployments) {
                String name = deployment.getMetadata().getName();
                Integer replicas = deployment.getSpec().getReplicas();
                System.out.println("- Deployment: " + name + " (副本数: " + replicas + ")");
            }
            
        } catch (Exception e) {
            System.err.println("列出Deployment时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
