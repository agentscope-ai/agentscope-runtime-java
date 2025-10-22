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

import java.util.HashMap;
import java.util.Map;

/**
 * 基础沙箱实现
 * 对应Python版本的BaseSandbox
 * 提供基本的IPython和Shell命令执行能力
 */
public class BaseSandbox extends Sandbox {
    
    /**
     * 构造函数
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public BaseSandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000);
    }
    
    /**
     * 构造函数
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param timeout 超时时间（秒）
     */
    public BaseSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        super(managerApi, userId, sessionId, SandboxType.BASE, timeout);
    }
    
    /**
     * 执行IPython代码
     * 
     * @param code Python代码
     * @return 执行结果
     */
    public String runIpythonCell(String code) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("code", code);
        return callTool("run_ipython_cell", arguments);
    }
    
    /**
     * 执行Shell命令
     * 
     * @param command Shell命令
     * @return 执行结果
     */
    public String runShellCommand(String command) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("command", command);
        return callTool("run_shell_command", arguments);
    }
}

