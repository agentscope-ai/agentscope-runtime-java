package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.CloseTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserCloser extends AgentScopeSandboxAwareTool<CloseTool> {
    public AsBrowserCloser() {
        super(new CloseTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_close();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser close error: " + e.getMessage()));
        }
    }
}
