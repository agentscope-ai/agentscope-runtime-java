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

import java.util.List;
import java.util.Map;

/**
 * 容器管理客户端基类
 * 定义了容器管理的基本接口，支持Docker和Kubernetes实现
 */
public abstract class BaseClient {
    
    /**
     * 连接容器管理服务
     * @return 连接状态
     */
    public abstract boolean connect();
    
    /**
     * 创建容器
     * @param containerName 容器名称
     * @param imageName 镜像名称
     * @param ports 端口列表
     * @param portMapping 端口映射
     * @param volumeBindings 卷绑定
     * @param environment 环境变量
     * @param runtimeConfig 运行时配置
     * @return 容器ID
     */
    public abstract String createContainer(String containerName, String imageName,
                                         List<String> ports, Map<String, Integer> portMapping,
                                         List<VolumeBinding> volumeBindings,
                                         Map<String, String> environment, String runtimeConfig);
    
    /**
     * 启动容器
     * @param containerId 容器ID
     */
    public abstract void startContainer(String containerId);
    
    /**
     * 停止容器
     * @param containerId 容器ID
     */
    public abstract void stopContainer(String containerId);
    
    /**
     * 删除容器
     * @param containerId 容器ID
     */
    public abstract void removeContainer(String containerId);
    
    /**
     * 获取容器状态
     * @param containerId 容器ID
     * @return 容器状态
     */
    public abstract String getContainerStatus(String containerId);
    
    /**
     * 停止并删除容器
     * @param containerId 容器ID
     */
    public void stopAndRemoveContainer(String containerId) {
        stopContainer(containerId);
        removeContainer(containerId);
    }
    
    /**
     * 检查连接状态
     * @return 是否已连接
     */
    public abstract boolean isConnected();
}
