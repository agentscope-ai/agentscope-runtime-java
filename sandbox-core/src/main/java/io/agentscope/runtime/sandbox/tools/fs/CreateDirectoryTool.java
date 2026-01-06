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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateDirectoryTool extends FsSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(CreateDirectoryTool.class);

    public CreateDirectoryTool() {
        super("fs_create_directory", "filesystem", "Create a new directory");
        schema = new HashMap<>();
        Map<String, Object> pathProperty = new HashMap<>();
        pathProperty.put("type", "string");
        pathProperty.put("description", "Path to the directory to create");

        Map<String, Object> properties = new HashMap<>();
        properties.put("path", pathProperty);

        List<String> required = List.of("path");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to create directory");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String fs_create_directory(String path) {
        try {
            if(sandbox instanceof FilesystemSandbox filesystemSandbox){
                return filesystemSandbox.createDirectory(path);
            }
            throw new RuntimeException("Only FilesystemSandbox supported in create directory tool");
        } catch (Exception e) {
            String errorMsg = "Create Directory Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }
}
