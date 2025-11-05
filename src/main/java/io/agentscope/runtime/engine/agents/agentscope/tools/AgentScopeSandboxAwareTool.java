package io.agentscope.runtime.engine.agents.agentscope.tools;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class AgentScopeSandboxAwareTool<T extends SandboxTool> implements AgentTool {
    protected T sandboxTool;

    public AgentScopeSandboxAwareTool(T sandboxTool) {
        this.sandboxTool = sandboxTool;
    }

    public String getName() {
        if (sandboxTool != null) {
            return sandboxTool.getName();
        }
        return null;
    }

    public String getDescription() {
        if (sandboxTool != null) {
            return sandboxTool.getDescription();
        }
        return null;
    }

    public Map<String, Object> getParameters() {
        if (sandboxTool != null) {
            return sandboxTool.getSchema();
        }
        return null;
    }

    public abstract Mono<ToolResultBlock> callAsync(Map<String, Object> input);

    public SandboxTool getSandboxTool() {
        return sandboxTool;
    }
}
