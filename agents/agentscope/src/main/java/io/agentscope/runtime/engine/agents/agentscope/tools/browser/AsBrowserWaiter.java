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
import io.agentscope.runtime.sandbox.tools.browser.WaitForTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserWaiter extends AgentScopeSandboxAwareTool<WaitForTool> {
    public AsBrowserWaiter() {
        super(new WaitForTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        Double time = input.containsKey("time") ? ((Number) input.get("time")).doubleValue() : null;
        String text = input.containsKey("text") ? (String) input.get("text") : null;
        String textGone = input.containsKey("textGone") ? (String) input.get("textGone") : null;

        try {
            String result = sandboxTool.browser_wait_for(time, text, textGone);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser wait error: " + e.getMessage()));
        }
    }
}
