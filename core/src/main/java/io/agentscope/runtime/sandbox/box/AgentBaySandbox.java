package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.AgentBayClient;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import java.util.Map;
import java.util.logging.Logger;


@RegisterSandbox(
        imageName = "agentbay-cloud",
        sandboxType = SandboxType.AGENTBAY,
        securityLevel = "high",
        timeout = 300,
        description = "AgentBay Cloud Sandbox Environment"
)
public class AgentBaySandbox extends CloudSandbox {

    private static final Logger logger = Logger.getLogger(AgentBaySandbox.class.getName());
    private String imageId;
    private Map<String, String> labels;

    public AgentBaySandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000, "linux_latest", null);
    }

    public AgentBaySandbox(SandboxManager managerApi, String userId, String sessionId,
                           int timeout, String imageId, Map<String, String> labels) {
        super(managerApi, userId, sessionId, timeout, SandboxType.AGENTBAY);
        try {
            ContainerModel containerModel = managerApi.createFromPool(sandboxType, userId, sessionId, imageId, labels);
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

    public Map<String, Object> getSessionInfo() {
        AgentBayClient agentBay = this.managerApi.getAgentBayClient();
        return agentBay.getSessionInfo(this.sandboxId);
    }

    public String getCloudProviderName(){
        return "AgentBay";
    }
}
