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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditFileTool extends FsSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(EditFileTool.class);

    public EditFileTool() {
        super("fs_edit_file", "filesystem", "Edit a file with find-replace operations");
        schema = new HashMap<>();
        
        Map<String, Object> pathProperty = new HashMap<>();
        pathProperty.put("type", "string");
        pathProperty.put("description", "Path to the file to edit");
        
        Map<String, Object> editsProperty = new HashMap<>();
        editsProperty.put("type", "array");
        editsProperty.put("description", "Array of edit objects with oldText and newText properties");

        Map<String, Object> properties = new HashMap<>();
        properties.put("path", pathProperty);
        properties.put("edits", editsProperty);

        List<String> required = Arrays.asList("path", "edits");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to edit file");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String fs_edit_file(String path, Map<String, Object>[] edits) {
        try {
            if(sandbox instanceof FilesystemSandbox filesystemSandbox){
                return filesystemSandbox.editFile(path, edits);
            }
            throw new RuntimeException("Only FilesystemSandbox supported in edit file tool");
        } catch (Exception e) {
            String errorMsg = "Edit File Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }
}
