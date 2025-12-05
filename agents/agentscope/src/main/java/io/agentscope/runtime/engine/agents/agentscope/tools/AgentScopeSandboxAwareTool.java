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

package io.agentscope.runtime.engine.agents.agentscope.tools;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class AgentScopeSandboxAwareTool<T extends SandboxTool> implements AgentTool {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    protected T sandboxTool;

    public AgentScopeSandboxAwareTool(T sandboxTool) {
        this.sandboxTool = sandboxTool;
    }

    public String getName() {
        if (sandboxTool != null) {
            return sandboxTool.getName();
        }
        return null;
    }

    public String getDescription() {
        if (sandboxTool != null) {
            return sandboxTool.getDescription();
        }
        return null;
    }

    public Map<String, Object> getParameters() {
        if (sandboxTool != null) {
            return sandboxTool.getSchema();
        }
        return null;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return callAsync(param.getInput());
    }

    public abstract Mono<ToolResultBlock> callAsync(Map<String, Object> input);

    public SandboxTool getSandboxTool() {
        return sandboxTool;
    }

    public void setSandbox(Sandbox sandbox) {
       this.sandboxTool.setSandbox(sandbox);
    }
}
