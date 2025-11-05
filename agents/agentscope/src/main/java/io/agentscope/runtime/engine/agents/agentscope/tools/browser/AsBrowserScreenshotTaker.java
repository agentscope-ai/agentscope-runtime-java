package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.TakeScreenshotTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserScreenshotTaker extends AgentScopeSandboxAwareTool<TakeScreenshotTool> {
    public AsBrowserScreenshotTaker() {
        super(new TakeScreenshotTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        Boolean raw = input.containsKey("raw") ? (Boolean) input.get("raw") : null;
        String filename = input.containsKey("filename") ? (String) input.get("filename") : null;
        String element = input.containsKey("element") ? (String) input.get("element") : null;
        String ref = input.containsKey("ref") ? (String) input.get("ref") : null;

        try {
            String result = sandboxTool.browser_take_screenshot(raw, filename, element, ref);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser take screenshot error: " + e.getMessage()));
        }
    }
}
