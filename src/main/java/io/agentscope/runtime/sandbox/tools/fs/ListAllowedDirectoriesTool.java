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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ListAllowedDirectoriesTool extends FsSandboxTool {

    Logger logger = Logger.getLogger(ListAllowedDirectoriesTool.class.getName());

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

    public String fs_list_allowed_directories(String userID, String sessionID) {
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
}
