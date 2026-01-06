package io.agentscope.runtime.sandbox.manager.model.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SandboxKey(String userID, String sessionID, String sandboxType) {
    private static final Logger logger = LoggerFactory.getLogger(SandboxKey.class);

    @Override
    public String toString() {
        return "SandboxKey{" + "userID='" + userID + '\'' + ", sessionID='" + sessionID + '\'' + ", sandboxType=" + sandboxType + '}';
    }
}
