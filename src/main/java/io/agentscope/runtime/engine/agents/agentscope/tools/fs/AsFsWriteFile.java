package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.WriteFileTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsWriteFile extends AgentScopeSandboxAwareTool<WriteFileTool> {
    public AsFsWriteFile() {
        super(new WriteFileTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        if (!input.containsKey("content")) {
            return Mono.just(ToolResultBlock.error("Error: key 'content' has to be contained in the input map"));
        }
        String path = (String) input.get("path");
        String content = (String) input.get("content");

        try {
            String result = sandboxTool.fs_write_file(path, content);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Write file error: " + e.getMessage()));
        }
    }
}
