package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.ConsoleMessagesTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserConsoleMessagesRetriever extends AgentScopeSandboxAwareTool<ConsoleMessagesTool> {
    public AsBrowserConsoleMessagesRetriever() {
        super(new ConsoleMessagesTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.browser_console_messages();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Get browser console messages error: " + e.getMessage()));
        }
    }
}
