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

public class SearchFilesTool extends SandboxTool {

    public SearchFilesTool() {
        super("fs_search_files", "filesystem", "Search for files matching a pattern");
        schema = new HashMap<>();
        
        Map<String, Object> pathProperty = new HashMap<>();
        pathProperty.put("type", "string");
        pathProperty.put("description", "Starting path for the search");
        
        Map<String, Object> patternProperty = new HashMap<>();
        patternProperty.put("type", "string");
        patternProperty.put("description", "Pattern to match files/directories");
        
        Map<String, Object> excludePatternsProperty = new HashMap<>();
        excludePatternsProperty.put("type", "array");
        excludePatternsProperty.put("description", "Patterns to exclude from search");

        Map<String, Object> properties = new HashMap<>();
        properties.put("path", pathProperty);
        properties.put("pattern", patternProperty);
        properties.put("excludePatterns", excludePatternsProperty);

        List<String> required = Arrays.asList("path", "pattern");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to search files");
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
                        new FileSearcher()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(FileSearcher.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理文件搜索的工具
     */
    class FileSearcher implements BiFunction<FileSearcher.Request, ToolContext, FileSearcher.Response> {

        Logger logger = Logger.getLogger(FileSearcher.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            try {
                String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
                String userID = userAndSession[0];
                String sessionID = userAndSession[1];
                
                String result = fs_search_files(request.path, request.pattern, request.excludePatterns, userID, sessionID);
                return new Response(result, "Filesystem search_files completed");
            } catch (Exception e) {
                return new Response("Error", "Filesystem search_files error: " + e.getMessage());
            }
        }

        private String fs_search_files(String path, String pattern, String[] excludePatterns, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof FilesystemSandbox filesystemSandbox) {
                    return filesystemSandbox.searchFiles(path, pattern, excludePatterns);
                }
                FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
                return filesystemSandbox.searchFiles(path, pattern, excludePatterns);
            } catch (Exception e) {
                String errorMsg = "Search Files Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty(required = true, value = "path")
                @JsonPropertyDescription("Starting path for the search")
                String path,
                @JsonProperty(required = true, value = "pattern")
                @JsonPropertyDescription("Pattern to match files/directories")
                String pattern,
                @JsonProperty(value = "excludePatterns")
                @JsonPropertyDescription("Patterns to exclude from search")
                String[] excludePatterns
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

