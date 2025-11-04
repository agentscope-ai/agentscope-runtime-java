package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.DirectoryTreeTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsDirectoryTree extends AgentScopeSandboxAwareTool<DirectoryTreeTool> {
    public AsFsDirectoryTree() {
        super(new DirectoryTreeTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        String path = (String) input.get("path");

        try {
            String result = sandboxTool.fs_directory_tree(path);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Get directory tree error: " + e.getMessage()));
        }
    }
}
