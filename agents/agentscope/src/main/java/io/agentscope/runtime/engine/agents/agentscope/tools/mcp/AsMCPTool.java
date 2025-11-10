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

package io.agentscope.runtime.engine.agents.agentscope.tools.mcp;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

public class AsMCPTool extends AgentScopeSandboxAwareTool<MCPTool> {
    public AsMCPTool(String name, String toolType, String description,
                     Map<String, Object> schema, Map<String, Object> serverConfigs,
                     SandboxType sandboxType, SandboxManager sandboxManager) {
        super(new MCPTool(name,
                toolType,
                description,
                schema,
                serverConfigs,
                sandboxType,
                sandboxManager));
    }

    public AsMCPTool(MCPTool mcpTool) {
        super(mcpTool);
    }

    @Override
    public Mono<ToolResultBlock> callAsync(Map<String, Object> input) {
        Map<String, Object> arguments = input != null ? input : Collections.emptyMap();

        try {
            String result = sandboxTool.executeMCPTool(arguments);
            return Mono.just(ToolResultBlock.text(result));
        } catch (Exception e) {
            return Mono.just(ToolResultBlock.error("MCP tool execution error: " + e.getMessage()));
        }
    }
}
