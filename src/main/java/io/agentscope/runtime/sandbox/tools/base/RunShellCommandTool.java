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
package io.agentscope.runtime.sandbox.tools.base;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RunShellCommandTool extends BaseSandboxTool {

    Logger logger = Logger.getLogger(RunShellCommandTool.class.getName());

    public RunShellCommandTool() {
        super("run_shell_command", "generic", "Execute shell commands and return the output or errors.");
        schema = new HashMap<>();
        Map<String, Object> commandProperty = new HashMap<>();
        commandProperty.put("type", "string");
        commandProperty.put("description", "Shell command to be executed");

        Map<String, Object> properties = new HashMap<>();
        properties.put("command", commandProperty);

        List<String> required = Arrays.asList("command");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to perform shell command execution");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String run_shell_command(String command) {
        try {
            logger.info("Run Shell Command: " + command);
            if(sandbox instanceof BaseSandbox baseSandbox){
                String result = baseSandbox.runShellCommand(command);
                logger.info("Execute Result: " + result);
                return result;
            }
            throw new RuntimeException("Only BaseSandbox supported in run shell command tool");
        } catch (Exception e) {
            String errorMsg = "Run Shell Command Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
