package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

public abstract class CloudSandbox extends Sandbox {
    public CloudSandbox(SandboxManager managerApi, String userId, String sessionId, SandboxType sandboxType) {
        this(managerApi, userId, sessionId, 3000, sandboxType);
    }

    public CloudSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout,
            SandboxType sandboxType) {
        super(managerApi, userId, sessionId, sandboxType, timeout);
    }
}