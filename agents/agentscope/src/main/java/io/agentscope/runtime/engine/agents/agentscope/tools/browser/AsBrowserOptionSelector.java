package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.SelectOptionTool;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class AsBrowserOptionSelector extends AgentScopeSandboxAwareTool<SelectOptionTool> {
    public AsBrowserOptionSelector() {
        super(new SelectOptionTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("element")) {
            return Mono.just(ToolResultBlock.error("Error: key 'element' has to be contained in the input map"));
        }
        if (!input.containsKey("ref")) {
            return Mono.just(ToolResultBlock.error("Error: key 'ref' has to be contained in the input map"));
        }
        if (!input.containsKey("values")) {
            return Mono.just(ToolResultBlock.error("Error: key 'values' has to be contained in the input map"));
        }
        String element = (String) input.get("element");
        String ref = (String) input.get("ref");
        Object valuesObj = input.get("values");
        String[] values;
        if (valuesObj instanceof List) {
            List<?> valuesList = (List<?>) valuesObj;
            values = valuesList.toArray(new String[0]);
        } else if (valuesObj instanceof String[]) {
            values = (String[]) valuesObj;
        } else {
            return Mono.just(ToolResultBlock.error("Error: 'values' must be an array of strings"));
        }

        try {
            String result = sandboxTool.browser_select_option(element, ref, values);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser select option error: " + e.getMessage()));
        }
    }
}
