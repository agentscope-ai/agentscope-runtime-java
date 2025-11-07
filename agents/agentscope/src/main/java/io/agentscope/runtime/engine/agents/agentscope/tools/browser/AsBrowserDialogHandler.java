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
import io.agentscope.runtime.sandbox.tools.browser.HandleDialogTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserDialogHandler extends AgentScopeSandboxAwareTool<HandleDialogTool> {
    public AsBrowserDialogHandler() {
        super(new HandleDialogTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("accept")) {
            return Mono.just(ToolResultBlock.error("Error: key 'accept' has to be contained in the input map"));
        }
        Boolean accept = (Boolean) input.get("accept");
        String promptText = input.containsKey("promptText") ? (String) input.get("promptText") : null;

        try {
            String result = sandboxTool.browser_handle_dialog(accept, promptText);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser handle dialog error: " + e.getMessage()));
        }
    }
}
