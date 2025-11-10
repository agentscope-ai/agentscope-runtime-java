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
import io.agentscope.runtime.sandbox.tools.browser.DragTool;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AsBrowserDragger extends AgentScopeSandboxAwareTool<DragTool> {
    public AsBrowserDragger() {
        super(new DragTool());
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        if (!input.containsKey("startElement")) {
            return Mono.just(ToolResultBlock.error("Error: key 'startElement' has to be contained in the input map"));
        }
        if (!input.containsKey("startRef")) {
            return Mono.just(ToolResultBlock.error("Error: key 'startRef' has to be contained in the input map"));
        }
        if (!input.containsKey("endElement")) {
            return Mono.just(ToolResultBlock.error("Error: key 'endElement' has to be contained in the input map"));
        }
        if (!input.containsKey("endRef")) {
            return Mono.just(ToolResultBlock.error("Error: key 'endRef' has to be contained in the input map"));
        }
        String startElement = (String) input.get("startElement");
        String startRef = (String) input.get("startRef");
        String endElement = (String) input.get("endElement");
        String endRef = (String) input.get("endRef");

        try {
            String result = sandboxTool.browser_drag(startElement, startRef, endElement, endRef);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error(
                    "Browser drag error: " + e.getMessage()));
        }
    }
}
