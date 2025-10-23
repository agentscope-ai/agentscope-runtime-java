package io.agentscope.runtime.sandbox.manager.util;

import java.util.Map;

public abstract class SandboxClient implements AutoCloseable {

    public abstract boolean checkHealth();

    public abstract void waitUntilHealthy();

    public abstract String callTool(String toolName, Map<String, Object> arguments);

    public abstract Map<String, Object> listTools(String toolType, Map<String, Object> arguments);

    @Override
    public void close() throws Exception {

    }
}
