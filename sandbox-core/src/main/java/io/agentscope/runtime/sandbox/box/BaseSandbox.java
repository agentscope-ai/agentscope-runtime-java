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

import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemStarter;
import io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemStarter;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import java.util.HashMap;
import java.util.Map;


@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest",
        sandboxType = "base",
        securityLevel = "medium",
        timeout = 30,
        description = "Base Sandbox"
)
public class BaseSandbox extends Sandbox {

    public BaseSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId
    ) {
        this(managerApi, userId, sessionId, Map.of());
    }

    public BaseSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemStarter fileSystemStarter
    ) {
        this(managerApi, userId, sessionId, fileSystemStarter, Map.of());
    }

    public BaseSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment
    ) {
        this(managerApi, userId, sessionId, LocalFileSystemStarter.builder().build(), environment);
    }

    public BaseSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemStarter fileSystemStarter,
            Map<String, String> environment
    ) {
        super(managerApi, userId, sessionId, "base", fileSystemStarter, environment);
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

