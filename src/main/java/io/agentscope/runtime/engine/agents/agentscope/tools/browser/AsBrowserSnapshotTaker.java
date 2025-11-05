package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.SnapshotTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserSnapshotTaker extends AgentScopeSandboxAwareTool<SnapshotTool> {
    public AsBrowserSnapshotTaker() {
        super(new SnapshotTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_snapshot();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser snapshot error: " + e.getMessage()));
        }
    }
}
