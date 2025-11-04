package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.NavigateBackTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserBackNavigator extends AgentScopeSandboxAwareTool<NavigateBackTool> {
    public AsBrowserBackNavigator() {
        super(new NavigateBackTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_navigate_back();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser navigate back error: " + e.getMessage()));
        }
    }
}
