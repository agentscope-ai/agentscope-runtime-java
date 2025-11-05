package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.TabNewTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserTabCreator extends AgentScopeSandboxAwareTool<TabNewTool> {
    public AsBrowserTabCreator() {
        super(new TabNewTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        String url = input.containsKey("url") ? (String) input.get("url") : null;

        try {
            String result = sandboxTool.browser_tab_new(url);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser tab create error: " + e.getMessage()));
        }
    }
}
