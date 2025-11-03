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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Browser drag tool
 */
public class DragTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(DragTool.class.getName());

    public DragTool() {
        super("browser_drag", "browser", "Drag and drop an element in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> startElementProperty = new HashMap<>();
        startElementProperty.put("type", "string");
        startElementProperty.put("description", "Human-readable source element description");
        
        Map<String, Object> startRefProperty = new HashMap<>();
        startRefProperty.put("type", "string");
        startRefProperty.put("description", "Exact source element reference from the page snapshot");
        
        Map<String, Object> endElementProperty = new HashMap<>();
        endElementProperty.put("type", "string");
        endElementProperty.put("description", "Human-readable target element description");
        
        Map<String, Object> endRefProperty = new HashMap<>();
        endRefProperty.put("type", "string");
        endRefProperty.put("description", "Exact target element reference from the page snapshot");

        Map<String, Object> properties = new HashMap<>();
        properties.put("startElement", startElementProperty);
        properties.put("startRef", startRefProperty);
        properties.put("endElement", endElementProperty);
        properties.put("endRef", endRefProperty);

        List<String> required = Arrays.asList("startElement", "startRef", "endElement", "endRef");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to drag and drop");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_drag(String startElement, String startRef, String endElement, String endRef, String userID, String sessionID) {
        try {
            if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                return browserSandbox.drag(startElement, startRef, endElement, endRef);
            }
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.drag(startElement, startRef, endElement, endRef);
        } catch (Exception e) {
            String errorMsg = "Browser Drag Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
