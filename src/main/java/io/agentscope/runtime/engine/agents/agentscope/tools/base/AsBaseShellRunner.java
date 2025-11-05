package io.agentscope.runtime.engine.agents.agentscope.tools.base;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.base.RunShellCommandTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBaseShellRunner extends AgentScopeSandboxAwareTool<RunShellCommandTool> {
    public AsBaseShellRunner() {
        super(new RunShellCommandTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("command")) {
            return Mono.just(ToolResultBlock.error("Error: key 'command' has to be contained in the input map"));
        }
        String command = (String) input.get("command");

        try {
            String result = sandboxTool.run_shell_command(command);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Execution error: " + e.getMessage()));
        }
    }
}
