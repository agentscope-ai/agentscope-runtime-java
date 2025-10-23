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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class ListAllowedDirectoriesTool extends SandboxTool {

    public ListAllowedDirectoriesTool() {
        super("fs_list_allowed_directories", "filesystem", "List allowed directories for the current session");
        schema = new HashMap<>();
        
        Map<String, Object> properties = new HashMap<>();

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to list allowed directories");
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
                        new AllowedDirectoriesLister()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(AllowedDirectoriesLister.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理允许目录列表获取的工具
     */
    class AllowedDirectoriesLister implements BiFunction<AllowedDirectoriesLister.Request, ToolContext, AllowedDirectoriesLister.Response> {

        Logger logger = Logger.getLogger(AllowedDirectoriesLister.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            try {
                String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
                String userID = userAndSession[0];
                String sessionID = userAndSession[1];
                
                String result = fs_list_allowed_directories(userID, sessionID);
                return new Response(result, "Filesystem list_allowed_directories completed");
            } catch (Exception e) {
                return new Response("Error", "Filesystem list_allowed_directories error: " + e.getMessage());
            }
        }

        private String fs_list_allowed_directories(String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof FilesystemSandbox filesystemSandbox) {
                    return filesystemSandbox.listAllowedDirectories();
                }
                FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
                return filesystemSandbox.listAllowedDirectories();
            } catch (Exception e) {
                String errorMsg = "List Allowed Directories Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request() { }

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

