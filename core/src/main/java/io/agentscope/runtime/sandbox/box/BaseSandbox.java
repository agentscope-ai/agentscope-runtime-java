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
package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import java.util.HashMap;
import java.util.Map;


@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest",
        sandboxType = SandboxType.BASE,
        securityLevel = "medium",
        timeout = 30,
        description = "Base Sandbox"
)
public class BaseSandbox extends Sandbox {

    public BaseSandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000);
    }

    public BaseSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        super(managerApi, userId, sessionId, SandboxType.BASE, timeout);
    }

    /**
     * Execute IPython code
     */
    public String runIpythonCell(String code) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("code", code);
        return callTool("run_ipython_cell", arguments);
    }

    /**
     * Execute shell command
     */
    public String runShellCommand(String command) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("command", command);
        return callTool("run_shell_command", arguments);
    }
}
