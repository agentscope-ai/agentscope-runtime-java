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

import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SearchFilesTool extends FsSandboxTool {

    Logger logger = Logger.getLogger(SearchFilesTool.class.getName());

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

    public String fs_search_files(String path, String pattern, String[] excludePatterns) {
        try {
            if(sandbox instanceof FilesystemSandbox filesystemSandbox){
                return filesystemSandbox.searchFiles(path, pattern, excludePatterns);
            }
            throw new RuntimeException("Only FilesystemSandbox supported in search files tool");
        } catch (Exception e) {
            String errorMsg = "Search Files Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
