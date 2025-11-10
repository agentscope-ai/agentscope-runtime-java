/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.engine.agents.agentscope.tools.fs;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.fs.EditFileTool;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class AsFsEditFile extends AgentScopeSandboxAwareTool<EditFileTool> {
    public AsFsEditFile() {
        super(new EditFileTool());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("path")) {
            return Mono.just(ToolResultBlock.error("Error: key 'path' has to be contained in the input map"));
        }
        if (!input.containsKey("edits")) {
            return Mono.just(ToolResultBlock.error("Error: key 'edits' has to be contained in the input map"));
        }
        String path = (String) input.get("path");
        Object editsObj = input.get("edits");
        Map<String, Object>[] edits;
        if (editsObj instanceof List) {
            List<?> editsList = (List<?>) editsObj;
            edits = editsList.toArray(new Map[0]);
        } else if (editsObj instanceof Map[]) {
            edits = (Map<String, Object>[]) editsObj;
        } else {
            return Mono.just(ToolResultBlock.error("Error: 'edits' must be an array of objects"));
        }

        try {
            String result = sandboxTool.fs_edit_file(path, edits);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Edit file error: " + e.getMessage()));
        }
    }
}
