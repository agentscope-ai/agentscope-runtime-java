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

public class ReadMultipleFilesTool extends FsSandboxTool {

    Logger logger = Logger.getLogger(ReadMultipleFilesTool.class.getName());

    public ReadMultipleFilesTool() {
        super("fs_read_multiple_files", "filesystem", "Read contents of multiple files");
        schema = new HashMap<>();
        
        Map<String, Object> pathsProperty = new HashMap<>();
        pathsProperty.put("type", "array");
        pathsProperty.put("description", "Paths to the files to read");

        Map<String, Object> properties = new HashMap<>();
        properties.put("paths", pathsProperty);

        List<String> required = Arrays.asList("paths");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to read multiple files");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String fs_read_multiple_files(String[] paths, String userID, String sessionID) {
        try {
            if (sandbox != null && sandbox instanceof FilesystemSandbox filesystemSandbox) {
                return filesystemSandbox.readMultipleFiles(java.util.Arrays.asList(paths));
            }
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.readMultipleFiles(java.util.Arrays.asList(paths));
        } catch (Exception e) {
            String errorMsg = "Read Multiple Files Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
