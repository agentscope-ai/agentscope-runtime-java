package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.SearchFilesTool;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class AsFsSearchFiles extends AgentScopeSandboxAwareTool<SearchFilesTool> {
    public AsFsSearchFiles() {
        super(new SearchFilesTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        if (!input.containsKey("pattern")) {
            return Mono.just(ToolResultBlock.error("Error: key 'pattern' has to be contained in the input map"));
        }
        String path = (String) input.get("path");
        String pattern = (String) input.get("pattern");
        String[] excludePatterns = null;
        if (input.containsKey("excludePatterns")) {
            Object excludePatternsObj = input.get("excludePatterns");
            if (excludePatternsObj instanceof List) {
                List<?> excludePatternsList = (List<?>) excludePatternsObj;
                excludePatterns = excludePatternsList.toArray(new String[0]);
            } else if (excludePatternsObj instanceof String[]) {
                excludePatterns = (String[]) excludePatternsObj;
            }
        }

        try {
            String result = sandboxTool.fs_search_files(path, pattern, excludePatterns);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Search files error: " + e.getMessage()));
        }
    }
}
