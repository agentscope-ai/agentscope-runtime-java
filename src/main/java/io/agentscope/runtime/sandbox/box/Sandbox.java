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
 * Sandbox base class
 * Corresponds to Python's Sandbox class
 * Routes all tool calls through SandboxManager to achieve sandbox isolation
 * 
 * <p>Usage patterns:
 * <ul>
 *   <li>Long-lived mode (recommended for Agents): Manual lifecycle management, requires explicit close()
 *   <li>Short-lived mode: Use try-with-resources for automatic cleanup
 * </ul>
 * 
 * <p>Example 1 - Long-lived (Agent scenario):
 * <pre>{@code
 * // Create in Agent constructor
 * Sandbox sandbox = new FilesystemSandbox(manager, userId, sessionId);
 * 
 * // Use in Agent's tool callbacks
 * String result = sandbox.callTool("read_file", args);
 * 
 * // Release when Agent is destroyed
 * sandbox.close();
 * }</pre>
 * 
 * <p>Example 2 - Short-lived:
 * <pre>{@code
 * try (Sandbox sandbox = new FilesystemSandbox(manager, userId, sessionId)) {
 *     String result = sandbox.callTool("read_file", args);
 * } // Auto-released
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
    protected final boolean autoRelease;
    private boolean closed = false;
    
    /**
     * Constructor (default: no auto-release, suitable for long-lived instances)
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
     * Constructor (can specify auto-release)
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
    
    public String getSandboxId() {
        return sandboxId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public SandboxType getSandboxType() {
        return sandboxType;
    }
    
    public ContainerModel getInfo() {
        return managerApi.getInfo(sandboxId, userId, sessionId);
    }
    
    public Map<String, Object> listTools(String toolType) {
        return managerApi.listTools(sandboxId, userId, sessionId, toolType);
    }
    
    public String callTool(String name, Map<String, Object> arguments) {
        return managerApi.callTool(sandboxId, userId, sessionId, name, arguments);
    }

    public Map<String, Object> addMcpServers(Map<String, Object> serverConfigs) {
        return addMcpServers(serverConfigs, false);
    }

    public Map<String, Object> addMcpServers(Map<String, Object> serverConfigs, boolean overwrite){
        return managerApi.addMcpServers(sandboxId, userId, sessionId, serverConfigs, overwrite);
    }
    
    /**
     * Close sandbox
     * If autoRelease=true, releases underlying container resources
     * If autoRelease=false, only marks as closed, sandbox remains available for reuse
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        
        try {
            if (autoRelease) {
                logger.info("Auto-releasing sandbox: " + sandboxId);
                managerApi.releaseSandbox(sandboxType, userId, sessionId);
            } else {
                logger.info("Sandbox closed (not released, can be reused): " + sandboxId);
            }
        } catch (Exception e) {
            logger.severe("Failed to cleanup sandbox: " + e.getMessage());
        }
    }
    
    /**
     * Manually release sandbox resources
     * Forces release of underlying container regardless of autoRelease setting
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
    
    public boolean isClosed() {
        return closed;
    }
}
