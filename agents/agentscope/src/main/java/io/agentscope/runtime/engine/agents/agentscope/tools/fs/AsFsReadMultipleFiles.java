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
import io.agentscope.runtime.sandbox.tools.fs.ReadMultipleFilesTool;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class AsFsReadMultipleFiles extends AgentScopeSandboxAwareTool<ReadMultipleFilesTool> {
    public AsFsReadMultipleFiles() {
        super(new ReadMultipleFilesTool());
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
            String result = sandboxTool.fs_read_multiple_files(paths);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Read multiple files error: " + e.getMessage()));
        }
    }
}
