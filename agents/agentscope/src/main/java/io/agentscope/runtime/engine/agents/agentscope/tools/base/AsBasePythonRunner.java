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

package io.agentscope.runtime.engine.agents.agentscope.tools.base;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.base.RunPythonTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBasePythonRunner extends AgentScopeSandboxAwareTool<RunPythonTool> {
    public AsBasePythonRunner() {
        super(new RunPythonTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        logger.info("AsBasePythonRunner called with input: {}", input);
        if (!input.containsKey("code")) {
            return Mono.just(ToolResultBlock.error("Error: key 'code' has to be contained in the input map"));
        }
        String code = (String) input.get("code");

        try {
            String result = sandboxTool.run_ipython_cell(code);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Calculation error: " + e.getMessage()));
        }
    }

}
