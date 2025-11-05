package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.ReadFileTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsReadFile extends AgentScopeSandboxAwareTool<ReadFileTool> {
    public AsFsReadFile() {
        super(new ReadFileTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        String path = (String) input.get("path");

        try {
            String result = sandboxTool.fs_read_file(path);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Read file error: " + e.getMessage()));
        }
    }
}
