package io.agentscope.runtime.example;

import io.agentscope.runtime.sandbox.manager.KubernetesClient;
import io.agentscope.runtime.sandbox.manager.model.VolumeBinding;

import java.util.*;

/**
 * KubernetesClient端口映射示例
 * 演示如何在Kubernetes中实现类似Docker的端口映射功能
 */
public class KubernetesClientPortMappingExample {
    
    public static void main(String[] args) {
        KubernetesClient client = new KubernetesClient();
        
        try {
            // 连接Kubernetes集群
            System.out.println("正在连接到Kubernetes集群...");
            if (!client.connect()) {
                System.err.println("无法连接到Kubernetes集群");
                return;
            }
            System.out.println("成功连接到Kubernetes集群\n");
            
            // 测试端口映射功能
            testPortMapping(client);
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试端口映射功能
     */
    private static void testPortMapping(KubernetesClient client) {
        System.out.println("=== 测试端口映射功能 ===");
        
        try {
            // 创建容器配置
            String containerName = "test-port-mapping-" + System.currentTimeMillis();
            String imageName = "nginx:latest";
            
            // 配置端口列表
            List<String> ports = Arrays.asList("80/TCP", "443/TCP");
            
            // 配置端口映射 (容器端口 -> 主机端口)
            Map<String, Integer> portMapping = new HashMap<>();
            portMapping.put("80/TCP", 8080);  // 容器80端口映射到主机8080端口
            portMapping.put("443/TCP", 8443); // 容器443端口映射到主机8443端口
            
            // 配置卷挂载
            List<VolumeBinding> volumeBindings = new ArrayList<>();
            volumeBindings.add(new VolumeBinding("/tmp/test", "/usr/share/nginx/html", "rw"));
            
            // 配置环境变量
            Map<String, String> environment = new HashMap<>();
            environment.put("NGINX_HOST", "localhost");
            environment.put("NGINX_PORT", "80");
            
            System.out.println("创建容器: " + containerName);
            System.out.println("镜像: " + imageName);
            System.out.println("端口映射: " + portMapping);
            System.out.println("卷挂载: " + volumeBindings);
            System.out.println("环境变量: " + environment);
            
            // 创建容器
            String containerId = client.createContainer(
                containerName, imageName, ports, portMapping, 
                volumeBindings, environment, null
            );
            
            System.out.println("容器创建成功，ID: " + containerId);
            
            // 等待容器启动
            Thread.sleep(5000);
            
            // 检查容器状态
            String status = client.getContainerStatus(containerId);
            System.out.println("容器状态: " + status);
            
            // 等待一段时间
            System.out.println("等待10秒...");
            Thread.sleep(10000);
            
            // 停止并删除容器
            System.out.println("停止并删除容器...");
            client.stopAndRemoveContainer(containerId);
            System.out.println("容器已删除");
            
        } catch (Exception e) {
            System.err.println("端口映射测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
