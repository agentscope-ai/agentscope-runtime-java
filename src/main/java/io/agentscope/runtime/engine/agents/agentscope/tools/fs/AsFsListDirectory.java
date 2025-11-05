package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.ListDirectoryTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsListDirectory extends AgentScopeSandboxAwareTool<ListDirectoryTool> {
    public AsFsListDirectory() {
        super(new ListDirectoryTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        String path = (String) input.get("path");

        try {
            String result = sandboxTool.fs_list_directory(path);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "List directory error: " + e.getMessage()));
        }
    }
}
