package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.NavigateTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserNavigator extends AgentScopeSandboxAwareTool<NavigateTool> {
    public AsBrowserNavigator() {
        super(new NavigateTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("url")) {
            return Mono.just(ToolResultBlock.error("Error: key 'url' has to be contained in the input map"));
        }
        String url = (String) input.get("url");

        try {
            String result = sandboxTool.browser_navigate(url);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser navigation error: " + e.getMessage()));
        }
    }
}
