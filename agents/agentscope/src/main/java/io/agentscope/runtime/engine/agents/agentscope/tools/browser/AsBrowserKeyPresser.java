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

package io.agentscope.runtime.engine.agents.agentscope.tools.browser;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.tools.browser.PressKeyTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserKeyPresser extends AgentScopeSandboxAwareTool<PressKeyTool> {
    public AsBrowserKeyPresser() {
        super(new PressKeyTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("key")) {
            return Mono.just(ToolResultBlock.error("Error: key 'key' has to be contained in the input map"));
        }
        String key = (String) input.get("key");

        try {
            String result = sandboxTool.browser_press_key(key);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser press key error: " + e.getMessage()));
        }
    }
}
