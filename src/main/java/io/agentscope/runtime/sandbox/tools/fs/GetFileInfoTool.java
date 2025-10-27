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
package io.agentscope.runtime.sandbox.tools.fs;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
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

public class GetFileInfoTool extends SandboxTool {

    public GetFileInfoTool() {
        super("fs_get_file_info", "filesystem", "Get information about a file or directory");
        schema = new HashMap<>();
        Map<String, Object> pathProperty = new HashMap<>();
        pathProperty.put("type", "string");
        pathProperty.put("description", "Path to the file or directory");

        Map<String, Object> properties = new HashMap<>();
        properties.put("path", pathProperty);

        List<String> required = Arrays.asList("path");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to get file information");
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
                        new FileInfoExecutor()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(FileInfoExecutor.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
    class FileInfoExecutor implements BiFunction<FileInfoExecutor.Request, ToolContext, FileInfoExecutor.Response> {

        Logger logger = Logger.getLogger(FileInfoExecutor.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            try {
                String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
                String userID = userAndSession[0];
                String sessionID = userAndSession[1];
                
                String result = fs_get_file_info(request.path, userID, sessionID);
                return new Response(result, "Filesystem get_file_info completed");
            } catch (Exception e) {
                return new Response("Error", "Filesystem get_file_info error: " + e.getMessage());
            }
        }

        private String fs_get_file_info(String path, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof FilesystemSandbox filesystemSandbox) {
                    return filesystemSandbox.getFileInfo(path);
                }
                FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
                return filesystemSandbox.getFileInfo(path);
            } catch (Exception e) {
                String errorMsg = "Get File Info Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty(required = true, value = "path")
                @JsonPropertyDescription("Path to the file or directory")
                String path
        ) { }

        @JsonClassDescription("The result contains filesystem tool output and execution message")
        public record Response(String result, String message) {
            public Response(String result, String message) { this.result = result; this.message = message; }
            @JsonProperty(required = true, value = "result")
            @JsonPropertyDescription("tool output")
            public String result() { return this.result; }
            @JsonProperty(required = true, value = "message")
            @JsonPropertyDescription("execute result")
            public String message() { return this.message; }
        }
    }
}

