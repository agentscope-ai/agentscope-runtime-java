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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class Sandbox implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Sandbox.class);

    protected final SandboxManager managerApi;
    protected String sandboxId;
    protected final String userId;
    protected final String sessionId;
    protected final SandboxType sandboxType;
    protected final int timeout;
    protected final boolean autoRelease;
    protected boolean closed = false;

    public Sandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            SandboxType sandboxType,
            int timeout) {
        this(managerApi, userId, sessionId, sandboxType, timeout, true);
    }

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

        if(sandboxType != SandboxType.AGENTBAY){
            try {
                ContainerModel containerModel = managerApi.createFromPool(sandboxType, userId, sessionId);
                if (containerModel == null) {
                    throw new RuntimeException(
                            "No sandbox available. Please check if sandbox images exist."
                    );
                }
                this.sandboxId = containerModel.getContainerName();
                logger.info("Sandbox initialized: {} (type={}, user={}, session={}, autoRelease={})", this.sandboxId, sandboxType, userId, sessionId, autoRelease);
            } catch (Exception e) {
                logger.error("Failed to initialize sandbox: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize sandbox", e);
            }
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
        return managerApi.getInfo(sandboxId);
    }

    public Map<String, Object> listTools() {
        return listTools(null);
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

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            if (autoRelease) {
                logger.info("Auto-releasing sandbox: {}", sandboxId);
                managerApi.releaseSandbox(sandboxType, userId, sessionId);
            } else {
                logger.info("Sandbox closed (not released, can be reused): {}", sandboxId);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup sandbox: {}", e.getMessage());
        }
    }

    /**
     * Manually release sandbox resources
     * Forces release of underlying container regardless of autoRelease setting
     */
    public void release() {
        try {
            logger.info("Manually releasing sandbox: {}", sandboxId);
            managerApi.releaseSandbox(sandboxType, userId, sessionId);
            closed = true;
        } catch (Exception e) {
            logger.error("Failed to release sandbox: {}", e.getMessage());
            throw new RuntimeException("Failed to release sandbox", e);
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
