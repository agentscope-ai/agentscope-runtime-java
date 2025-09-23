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
import org.springframework.ai.chat.model.ToolContext;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Call sandbox to run shell commands
 *
 * @author xuehuitian45
 * @since 2025/9/8
 */
public class RunShellCommandTool implements BiFunction<RunShellCommandTool.RunShellCommandToolRequest, ToolContext, RunShellCommandTool.RunShellCommandToolResponse> {
    Logger logger = Logger.getLogger(RunShellCommandTool.class.getName());

    @Override
    public RunShellCommandTool.RunShellCommandToolResponse apply(RunShellCommandTool.RunShellCommandToolRequest request, ToolContext toolContext) {
        try {
            String result = performShellExecute(
                    request.command
            );

            return new RunShellCommandTool.RunShellCommandToolResponse(
                    new RunShellCommandTool.Response(result, "Shell execution completed")
            );
        } catch (Exception e) {
            return new RunShellCommandTool.RunShellCommandToolResponse(
                    new RunShellCommandTool.Response("Error", "Shell execution error : " + e.getMessage())
            );
        }
    }


    private String performShellExecute(String command) {
        logger.info("Run Shell Command: " + command);
        SandboxTools tools = new SandboxTools();
        String result = tools.run_shell_command(command);
        logger.info("Execute Result: " + result);
        return result;
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
    public record RunShellCommandToolResponse(@JsonProperty("Response") RunShellCommandTool.Response output) {
        public RunShellCommandToolResponse(RunShellCommandTool.Response output) {
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
