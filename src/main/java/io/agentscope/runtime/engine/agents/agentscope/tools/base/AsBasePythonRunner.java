package io.agentscope.runtime.engine.agents.agentscope.tools.base;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.base.RunPythonTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBasePythonRunner extends AgentScopeSandboxAwareTool<RunPythonTool> {
    public AsBasePythonRunner() {
        super(new RunPythonTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("code")) {
            return Mono.just(ToolResultBlock.error("Error: key 'code' has to be contained in the input map"));
        }
        String code = (String) input.get("code");

        try {
            String result = sandboxTool.run_ipython_cell(code);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Calculation error: " + e.getMessage()));
        }
    }
}
