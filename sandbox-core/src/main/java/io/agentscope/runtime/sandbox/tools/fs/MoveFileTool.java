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

public class MoveFileTool extends FsSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(MoveFileTool.class);

    public MoveFileTool() {
        super("fs_move_file", "filesystem", "Move or rename a file or directory");
        schema = new HashMap<>();
        
        Map<String, Object> sourceProperty = new HashMap<>();
        sourceProperty.put("type", "string");
        sourceProperty.put("description", "Source path to move from");
        
        Map<String, Object> destinationProperty = new HashMap<>();
        destinationProperty.put("type", "string");
        destinationProperty.put("description", "Destination path to move to");

        Map<String, Object> properties = new HashMap<>();
        properties.put("source", sourceProperty);
        properties.put("destination", destinationProperty);

        List<String> required = Arrays.asList("source", "destination");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to move file");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String fs_move_file(String source, String destination) {
        try {
            if(sandbox instanceof FilesystemSandbox filesystemSandbox){
                return filesystemSandbox.moveFile(source, destination);
            }
            throw new RuntimeException("Only FilesystemSandbox supported in move file tool");
        } catch (Exception e) {
            String errorMsg = "Move File Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }
}
