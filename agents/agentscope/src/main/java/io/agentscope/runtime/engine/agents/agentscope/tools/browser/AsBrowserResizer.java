package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.ResizeTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserResizer extends AgentScopeSandboxAwareTool<ResizeTool> {
    public AsBrowserResizer() {
        super(new ResizeTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("width")) {
            return Mono.just(ToolResultBlock.error("Error: key 'width' has to be contained in the input map"));
        }
        if (!input.containsKey("height")) {
            return Mono.just(ToolResultBlock.error("Error: key 'height' has to be contained in the input map"));
        }
        Double width = ((Number) input.get("width")).doubleValue();
        Double height = ((Number) input.get("height")).doubleValue();

        try {
            String result = sandboxTool.browser_resize(width, height);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser resize error: " + e.getMessage()));
        }
    }
}
