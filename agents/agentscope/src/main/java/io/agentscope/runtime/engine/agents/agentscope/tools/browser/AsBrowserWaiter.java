package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.WaitForTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserWaiter extends AgentScopeSandboxAwareTool<WaitForTool> {
    public AsBrowserWaiter() {
        super(new WaitForTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        Double time = input.containsKey("time") ? ((Number) input.get("time")).doubleValue() : null;
        String text = input.containsKey("text") ? (String) input.get("text") : null;
        String textGone = input.containsKey("textGone") ? (String) input.get("textGone") : null;

        try {
            String result = sandboxTool.browser_wait_for(time, text, textGone);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser wait error: " + e.getMessage()));
        }
    }
}
