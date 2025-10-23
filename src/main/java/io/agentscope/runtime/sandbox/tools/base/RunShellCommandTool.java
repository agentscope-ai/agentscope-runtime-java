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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Call sandbox to run shell commands
 *
 * @author xuehuitian45
 * @since 2025/9/8
 */
public class RunShellCommandTool extends SandboxTool {
    
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

    @Override
    public ToolCallback buildTool() {
        ObjectMapper mapper = new ObjectMapper();
        String inputSchema = "";
        try {
            inputSchema = mapper.writeValueAsString(schema);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return FunctionToolCallback
                .builder(
                        name,
                        new ShellExecutor()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(ShellExecutor.RunShellCommandToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理Shell命令执行的工具
     */
    class ShellExecutor implements BiFunction<ShellExecutor.RunShellCommandToolRequest, ToolContext, ShellExecutor.RunShellCommandToolResponse> {
        
        Logger logger = Logger.getLogger(ShellExecutor.class.getName());

        @Override
        public RunShellCommandToolResponse apply(RunShellCommandToolRequest request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            try {
                String result = performShellExecute(
                        request.command,
                        userID,
                        sessionID
                );

                return new RunShellCommandToolResponse(
                        new Response(result, "Shell execution completed")
                );
            } catch (Exception e) {
                return new RunShellCommandToolResponse(
                        new Response("Error", "Shell execution error : " + e.getMessage())
                );
            }
        }


        private String performShellExecute(String command, String userID, String sessionID) {
            return run_shell_command(command, userID, sessionID);
        }

        private String run_shell_command(String command, String userID, String sessionID) {
            try {
                logger.info("Run Shell Command: " + command);
                if (sandbox != null && sandbox instanceof BaseSandbox baseSandbox) {
                    return baseSandbox.runShellCommand(command);
                }
                BaseSandbox baseSandbox = new BaseSandbox(sandboxManager, userID, sessionID);
                String result = baseSandbox.runShellCommand(command);
                logger.info("Execute Result: " + result);
                return result;
            } catch (Exception e) {
                String errorMsg = "Run Shell Command Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        // Request type definition
        public record RunShellCommandToolRequest(
                @JsonProperty(required = true, value = "command")
                @JsonPropertyDescription("Shell command to be executed")
                String command
        ) {
            public RunShellCommandToolRequest(String command) {
                this.command = command;
            }
        }

        // Response type definition
        public record RunShellCommandToolResponse(@JsonProperty("Response") Response output) {
            public RunShellCommandToolResponse(Response output) {
                this.output = output;
            }
        }

        @JsonClassDescription("The result contains the shell output and the execute result")
        public record Response(String result, String message) {
            public Response(String result, String message) {
                this.result = result;
                this.message = message;
            }

            @JsonProperty(required = true, value = "result")
            @JsonPropertyDescription("shell output")
            public String result() {
                return this.result;
            }

            @JsonProperty(required = true, value = "message")
            @JsonPropertyDescription("execute result")
            public String message() {
                return this.message;
            }
        }
    }
}
