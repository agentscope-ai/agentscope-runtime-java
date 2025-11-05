package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.GetFileInfoTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsGetFileInfo extends AgentScopeSandboxAwareTool<GetFileInfoTool> {
    public AsFsGetFileInfo() {
        super(new GetFileInfoTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        String path = (String) input.get("path");

        try {
            String result = sandboxTool.fs_get_file_info(path);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Get file info error: " + e.getMessage()));
        }
    }
}
