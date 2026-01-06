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
package io.agentscope.runtime.sandbox.tools.browser;

import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Browser file upload tool
 */
public class FileUploadTool extends BrowserSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadTool.class);

    public FileUploadTool() {
        super("browser_file_upload", "browser", "Upload files in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> pathsProperty = new HashMap<>();
        pathsProperty.put("type", "array");
        pathsProperty.put("description", "The absolute paths to the files to upload");

        Map<String, Object> properties = new HashMap<>();
        properties.put("paths", pathsProperty);

        List<String> required = List.of("paths");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to upload files");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_file_upload(String[] paths) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.fileUpload(paths);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser file upload tool");
        } catch (Exception e) {
            String errorMsg = "Browser File Upload Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }
}
