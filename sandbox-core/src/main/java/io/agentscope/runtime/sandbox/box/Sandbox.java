package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemStarter;
import io.agentscope.runtime.sandbox.manager.fs.LocalFileSystemStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Sandbox implements AutoCloseable, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Sandbox.class);

    protected transient final SandboxService managerApi;
    protected String sandboxId;
    protected final String userId;
    protected final String sessionId;
    protected final String sandboxType;
    protected boolean closed = false;
    protected Map<String, String> environment;
    protected FileSystemStarter fileSystemStarter;

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

    public FileSystemStarter getFileSystemConfig() {
        return fileSystemStarter;
    }

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
            if(!managerApi.stopAndRemoveSandbox(sandboxId)){
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
