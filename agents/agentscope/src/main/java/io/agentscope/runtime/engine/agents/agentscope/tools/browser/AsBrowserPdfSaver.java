package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.PdfSaveTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserPdfSaver extends AgentScopeSandboxAwareTool<PdfSaveTool> {
    public AsBrowserPdfSaver() {
        super(new PdfSaveTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("filename")) {
            return Mono.just(ToolResultBlock.error("Error: key 'filename' has to be contained in the input map"));
        }
        String filename = (String) input.get("filename");

        try {
            String result = sandboxTool.browser_pdf_save(filename);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser PDF save error: " + e.getMessage()));
        }
    }
}
