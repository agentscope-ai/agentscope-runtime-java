package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.MoveFileTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsFsMoveFile extends AgentScopeSandboxAwareTool<MoveFileTool> {
    public AsFsMoveFile() {
        super(new MoveFileTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("source")) {
            return Mono.just(ToolResultBlock.error("Error: key 'source' has to be contained in the input map"));
        }
        if (!input.containsKey("destination")) {
            return Mono.just(ToolResultBlock.error("Error: key 'destination' has to be contained in the input map"));
        }
        String source = (String) input.get("source");
        String destination = (String) input.get("destination");

        try {
            String result = sandboxTool.fs_move_file(source, destination);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Move file error: " + e.getMessage()));
        }
    }
}
