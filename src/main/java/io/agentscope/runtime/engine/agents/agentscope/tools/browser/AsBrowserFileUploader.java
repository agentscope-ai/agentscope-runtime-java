package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.FileUploadTool;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class AsBrowserFileUploader extends AgentScopeSandboxAwareTool<FileUploadTool> {
    public AsBrowserFileUploader() {
        super(new FileUploadTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("paths")) {
            return Mono.just(ToolResultBlock.error("Error: key 'paths' has to be contained in the input map"));
        }
        Object pathsObj = input.get("paths");
        String[] paths;
        if (pathsObj instanceof List) {
            List<?> pathsList = (List<?>) pathsObj;
            paths = pathsList.toArray(new String[0]);
        } else if (pathsObj instanceof String[]) {
            paths = (String[]) pathsObj;
        } else {
            return Mono.just(ToolResultBlock.error("Error: 'paths' must be an array of strings"));
        }

        try {
            String result = sandboxTool.browser_file_upload(paths);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser file upload error: " + e.getMessage()));
        }
    }
}
