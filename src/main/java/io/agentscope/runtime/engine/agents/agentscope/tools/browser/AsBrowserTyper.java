package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.TypeTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserTyper extends AgentScopeSandboxAwareTool<TypeTool> {
    public AsBrowserTyper() {
        super(new TypeTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("element")) {
            return Mono.just(ToolResultBlock.error("Error: key 'element' has to be contained in the input map"));
        }
        if (!input.containsKey("ref")) {
            return Mono.just(ToolResultBlock.error("Error: key 'ref' has to be contained in the input map"));
        }
        if (!input.containsKey("text")) {
            return Mono.just(ToolResultBlock.error("Error: key 'text' has to be contained in the input map"));
        }
        String element = (String) input.get("element");
        String ref = (String) input.get("ref");
        String text = (String) input.get("text");
        Boolean submit = input.containsKey("submit") ? (Boolean) input.get("submit") : false;
        Boolean slowly = input.containsKey("slowly") ? (Boolean) input.get("slowly") : false;

        try {
            String result = sandboxTool.browser_type(element, ref, text, submit, slowly);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser type error: " + e.getMessage()));
        }
    }
}
