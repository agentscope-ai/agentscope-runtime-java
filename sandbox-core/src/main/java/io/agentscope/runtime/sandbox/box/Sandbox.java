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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemStarter;
import io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Sandbox implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Sandbox.class);

    protected SandboxService managerApi;
    protected String sandboxId;
    protected String userId;
    protected String sessionId;
    protected String sandboxType;
    protected boolean closed = false;
    protected Map<String, String> environment;
    protected FileSystemStarter fileSystemStarter;

    @JsonCreator
    public Sandbox(
            @JsonProperty("sandboxId") String sandboxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("sandboxType") String sandboxType,
            @JsonProperty("fileSystemStarter") FileSystemStarter fileSystemStarter,
            @JsonProperty("environment") Map<String, String> environment,
            @JsonProperty("closed") boolean closed
    ) {
        this.sandboxId = sandboxId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.sandboxType = sandboxType;
        this.fileSystemStarter = fileSystemStarter;
        this.environment = environment != null ? new HashMap<>(environment) : new HashMap<>();
        this.closed = closed;
    }

    public Sandbox(SandboxService managerApi,
                   String userId,
                   String sessionId,
                   String sandboxType
    ) {
        this(managerApi, userId, sessionId, sandboxType, LocalFileSystemStarter.builder().build(), Map.of());
    }

    public Sandbox(SandboxService managerApi,
                   String userId,
                   String sessionId,
                   String sandboxType,
                   FileSystemStarter fileSystemStarter
    ) {
        this(managerApi, userId, sessionId, sandboxType, fileSystemStarter, Map.of());
    }

    public Sandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String sandboxType,
            Map<String, String> environment
    ) {
        this(managerApi, userId, sessionId, sandboxType, LocalFileSystemStarter.builder().build(), environment);
    }

    public Sandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String sandboxType,
            FileSystemStarter fileSystemStarter,
            Map<String, String> environment
    ) {
        this.managerApi = managerApi;
        this.userId = userId;
        this.sessionId = sessionId;
        this.sandboxType = sandboxType;
        this.fileSystemStarter = fileSystemStarter;
        this.environment = new HashMap<>(environment);

        try {
            ContainerModel containerModel = managerApi.createContainer(this);
            if (containerModel == null) {
                throw new RuntimeException(
                        "No sandbox available. Please check if sandbox images exist."
                );
            }
            this.sandboxId = containerModel.getContainerId();
            logger.info("Sandbox initialized: {} (type={}, user={}, session={})", this.sandboxId, sandboxType, userId, sessionId);
        } catch (Exception e) {
            logger.error("Failed to initialize sandbox: {}", e.getMessage());
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

    public String getSandboxType() {
        return sandboxType;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public FileSystemStarter getFileSystemStarter() {
        return fileSystemStarter;
    }

    @JsonIgnore
    public ContainerModel getInfo() {
        return managerApi.getInfo(sandboxId);
    }

    public Map<String, Object> listTools() {
        return listTools(null);
    }

    public Map<String, Object> listTools(String toolType) {
        return managerApi.listTools(sandboxId, toolType);
    }

    public String callTool(String name, Map<String, Object> arguments) {
        return managerApi.callTool(sandboxId, name, arguments);
    }

    public Map<String, Object> addMcpServers(Map<String, Object> serverConfigs) {
        return addMcpServers(serverConfigs, false);
    }

    public Map<String, Object> addMcpServers(Map<String, Object> serverConfigs, boolean overwrite) {
        return managerApi.addMcpServers(sandboxId, serverConfigs, overwrite);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            logger.info("Auto-releasing sandbox: {}", sandboxId);
            if (!managerApi.stopAndRemoveSandbox(sandboxId)) {
                logger.warn("Sandbox {} failed to remove", sandboxId);
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
        close();
    }

    public boolean isClosed() {
        return closed;
    }
}
