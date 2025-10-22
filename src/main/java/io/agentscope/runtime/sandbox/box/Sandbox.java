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
package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Sandbox接口基类
 * 对应Python版本的Sandbox类
 * 通过SandboxManager中转所有工具调用，实现沙箱隔离
 * 
 * <p>使用模式：
 * <ul>
 *   <li>长期持有模式（推荐用于Agent）：手动管理生命周期，需要显式调用close()
 *   <li>短期使用模式：使用try-with-resources自动清理
 * </ul>
 * 
 * <p>示例1 - 长期持有（Agent场景）：
 * <pre>{@code
 * // 在Agent构造函数中创建
 * Sandbox sandbox = new FilesystemSandbox(manager, userId, sessionId);
 * 
 * // 在Agent的工具回调中使用
 * String result = sandbox.callTool("read_file", args);
 * 
 * // 在Agent销毁时释放
 * sandbox.close();
 * }</pre>
 * 
 * <p>示例2 - 短期使用：
 * <pre>{@code
 * try (Sandbox sandbox = new FilesystemSandbox(manager, userId, sessionId)) {
 *     String result = sandbox.callTool("read_file", args);
 * } // 自动释放
 * }</pre>
 */
public abstract class Sandbox implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(Sandbox.class.getName());
    
    protected final SandboxManager managerApi;
    protected final String sandboxId;
    protected final String userId;
    protected final String sessionId;
    protected final SandboxType sandboxType;
    protected final int timeout;
    protected final boolean autoRelease; // 是否自动释放
    private boolean closed = false; // 防止重复关闭
    
    /**
     * 构造函数（默认不自动释放，适合长期持有）
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param sandboxType 沙箱类型
     * @param timeout 超时时间（秒）
     */
    public Sandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            SandboxType sandboxType,
            int timeout) {
        this(managerApi, userId, sessionId, sandboxType, timeout, false);
    }
    
    /**
     * 构造函数（可指定是否自动释放）
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param sandboxType 沙箱类型
     * @param timeout 超时时间（秒）
     * @param autoRelease 是否在close时自动释放沙箱资源
     */
    public Sandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            SandboxType sandboxType,
            int timeout,
            boolean autoRelease) {
        this.managerApi = managerApi;
        this.userId = userId;
        this.sessionId = sessionId;
        this.sandboxType = sandboxType;
        this.timeout = timeout;
        this.autoRelease = autoRelease;
        
        // 从池中获取或创建沙箱（如果已存在会复用）
        try {
            ContainerModel containerModel = managerApi.createFromPool(sandboxType, userId, sessionId);
            if (containerModel == null) {
                throw new RuntimeException(
                    "No sandbox available. Please check if sandbox images exist."
                );
            }
            this.sandboxId = containerModel.getContainerName();
            logger.info("Sandbox initialized: " + this.sandboxId + 
                       " (type=" + sandboxType + ", user=" + userId + 
                       ", session=" + sessionId + ", autoRelease=" + autoRelease + ")");
        } catch (Exception e) {
            logger.severe("Failed to initialize sandbox: " + e.getMessage());
            throw new RuntimeException("Failed to initialize sandbox", e);
        }
    }
    
    /**
     * 获取沙箱ID
     */
    public String getSandboxId() {
        return sandboxId;
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * 获取沙箱类型
     */
    public SandboxType getSandboxType() {
        return sandboxType;
    }
    
    /**
     * 获取沙箱信息
     */
    public ContainerModel getInfo() {
        return managerApi.getInfo(sandboxId, userId, sessionId);
    }
    
    /**
     * 列出可用工具
     * 
     * @param toolType 工具类型（可选）
     * @return 工具列表
     */
    public Map<String, Object> listTools(String toolType) {
        return managerApi.listTools(sandboxId, userId, sessionId, toolType);
    }
    
    /**
     * 调用工具
     * 
     * @param name 工具名称
     * @param arguments 工具参数
     * @return 执行结果
     */
    public String callTool(String name, Map<String, Object> arguments) {
        return managerApi.callTool(sandboxId, userId, sessionId, name, arguments);
    }
    
    /**
     * 关闭沙箱
     * 如果autoRelease=true，会释放底层容器资源
     * 如果autoRelease=false，只是标记为已关闭，沙箱仍然可用（供其他实例复用）
     */
    @Override
    public void close() {
        if (closed) {
            return; // 防止重复关闭
        }
        
        closed = true;
        
        try {
            if (autoRelease) {
                // 自动释放模式：销毁沙箱容器
                logger.info("Auto-releasing sandbox: " + sandboxId);
                managerApi.releaseSandbox(sandboxType, userId, sessionId);
            } else {
                // 手动管理模式：仅标记关闭，不释放容器
                logger.info("Sandbox closed (not released, can be reused): " + sandboxId);
            }
        } catch (Exception e) {
            logger.severe("Failed to cleanup sandbox: " + e.getMessage());
        }
    }
    
    /**
     * 手动释放沙箱资源
     * 无论autoRelease设置如何，都会强制释放底层容器
     */
    public void release() {
        try {
            logger.info("Manually releasing sandbox: " + sandboxId);
            managerApi.releaseSandbox(sandboxType, userId, sessionId);
            closed = true;
        } catch (Exception e) {
            logger.severe("Failed to release sandbox: " + e.getMessage());
            throw new RuntimeException("Failed to release sandbox", e);
        }
    }
    
    /**
     * 检查沙箱是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
}

