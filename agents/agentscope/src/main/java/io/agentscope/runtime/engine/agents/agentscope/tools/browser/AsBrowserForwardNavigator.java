package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.NavigateForwardTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserForwardNavigator extends AgentScopeSandboxAwareTool<NavigateForwardTool> {
    public AsBrowserForwardNavigator() {
        super(new NavigateForwardTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_navigate_forward();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser navigate forward error: " + e.getMessage()));
        }
    }
}
