package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.TabListTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserTabLister extends AgentScopeSandboxAwareTool<TabListTool> {
    public AsBrowserTabLister() {
        super(new TabListTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_tab_list();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Get browser tab list error: " + e.getMessage()));
        }
    }
}
