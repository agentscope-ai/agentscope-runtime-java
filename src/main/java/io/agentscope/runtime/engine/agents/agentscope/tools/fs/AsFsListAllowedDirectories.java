package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.ListAllowedDirectoriesTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsListAllowedDirectories extends AgentScopeSandboxAwareTool<ListAllowedDirectoriesTool> {
    public AsFsListAllowedDirectories() {
        super(new ListAllowedDirectoriesTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        try {
            String result = sandboxTool.fs_list_allowed_directories();
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "List allowed directories error: " + e.getMessage()));
        }
    }
}
