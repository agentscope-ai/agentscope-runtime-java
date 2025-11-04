package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.PressKeyTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserKeyPresser extends AgentScopeSandboxAwareTool<PressKeyTool> {
    public AsBrowserKeyPresser() {
        super(new PressKeyTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("key")) {
            return Mono.just(ToolResultBlock.error("Error: key 'key' has to be contained in the input map"));
        }
        String key = (String) input.get("key");

        try {
            String result = sandboxTool.browser_press_key(key);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser press key error: " + e.getMessage()));
        }
    }
}
