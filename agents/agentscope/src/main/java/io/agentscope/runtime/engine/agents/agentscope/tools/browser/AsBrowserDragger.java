package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.DragTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserDragger extends AgentScopeSandboxAwareTool<DragTool> {
    public AsBrowserDragger() {
        super(new DragTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("startElement")) {
            return Mono.just(ToolResultBlock.error("Error: key 'startElement' has to be contained in the input map"));
        }
        if (!input.containsKey("startRef")) {
            return Mono.just(ToolResultBlock.error("Error: key 'startRef' has to be contained in the input map"));
        }
        if (!input.containsKey("endElement")) {
            return Mono.just(ToolResultBlock.error("Error: key 'endElement' has to be contained in the input map"));
        }
        if (!input.containsKey("endRef")) {
            return Mono.just(ToolResultBlock.error("Error: key 'endRef' has to be contained in the input map"));
        }
        String startElement = (String) input.get("startElement");
        String startRef = (String) input.get("startRef");
        String endElement = (String) input.get("endElement");
        String endRef = (String) input.get("endRef");

        try {
            String result = sandboxTool.browser_drag(startElement, startRef, endElement, endRef);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser drag error: " + e.getMessage()));
        }
    }
}
