package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.TabCloseTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserTabCloser extends AgentScopeSandboxAwareTool<TabCloseTool> {
    public AsBrowserTabCloser() {
        super(new TabCloseTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        Integer index = input.containsKey("index") ? ((Number) input.get("index")).intValue() : null;

        try {
            String result = sandboxTool.browser_tab_close(index);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser tab close error: " + e.getMessage()));
        }
    }
}
