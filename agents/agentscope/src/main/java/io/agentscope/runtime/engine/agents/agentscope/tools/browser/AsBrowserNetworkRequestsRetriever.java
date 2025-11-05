package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.NetworkRequestsTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserNetworkRequestsRetriever extends AgentScopeSandboxAwareTool<NetworkRequestsTool> {
    public AsBrowserNetworkRequestsRetriever() {
        super(new NetworkRequestsTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_network_requests();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Get browser network requests error: " + e.getMessage()));
        }
    }
}
