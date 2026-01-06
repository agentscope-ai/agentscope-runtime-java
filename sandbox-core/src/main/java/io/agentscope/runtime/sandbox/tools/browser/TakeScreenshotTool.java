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
import java.util.Map;

public class TakeScreenshotTool extends BrowserSandboxTool {
    private static final Logger logger = LoggerFactory.getLogger(TakeScreenshotTool.class);

    public TakeScreenshotTool() {
        super("browser_take_screenshot", "browser", "Take a screenshot of the browser");
        schema = new HashMap<>();
        
        Map<String, Object> rawProperty = new HashMap<>();
        rawProperty.put("type", "boolean");
        rawProperty.put("description", "Whether to return raw image data");
        
        Map<String, Object> filenameProperty = new HashMap<>();
        filenameProperty.put("type", "string");
        filenameProperty.put("description", "Filename to save screenshot");
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Element description for partial screenshot");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Element reference for partial screenshot");

        Map<String, Object> properties = new HashMap<>();
        properties.put("raw", rawProperty);
        properties.put("filename", filenameProperty);
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to take screenshot");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_take_screenshot(Boolean raw, String filename, String element, String ref) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.takeScreenshot(raw, filename, element, ref);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser take screenshot tool");
        } catch (Exception e) {
            String errorMsg = "Browser Take Screenshot Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }

}
