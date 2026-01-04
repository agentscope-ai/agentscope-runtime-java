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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Browser hover tool
 */
public class HoverTool extends BrowserSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(HoverTool.class);

    public HoverTool() {
        super("browser_hover", "browser", "Hover over an element in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Human-readable element description");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Exact target element reference from the page snapshot");

        Map<String, Object> properties = new HashMap<>();
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);

        List<String> required = Arrays.asList("element", "ref");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to hover over element");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_hover(String element, String ref) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.hover(element, ref);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser hover tool");
        } catch (Exception e) {
            String errorMsg = "Browser Hover Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }
}
