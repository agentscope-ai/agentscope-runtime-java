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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Browser PDF save tool
 */
public class PdfSaveTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(PdfSaveTool.class.getName());

    public PdfSaveTool() {
        super("browser_pdf_save", "browser", "Save the current page as PDF");
        schema = new HashMap<>();
        
        Map<String, Object> filenameProperty = new HashMap<>();
        filenameProperty.put("type", "string");
        filenameProperty.put("description", "File name to save the pdf to");

        Map<String, Object> properties = new HashMap<>();
        properties.put("filename", filenameProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to save PDF");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_pdf_save(String filename) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.pdfSave(filename);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser pdf save tool");
        } catch (Exception e) {
            String errorMsg = "Browser PDF Save Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
