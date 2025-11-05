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
 * Browser dropdown selection tool
 */
public class SelectOptionTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(SelectOptionTool.class.getName());

    public SelectOptionTool() {
        super("browser_select_option", "browser", "Select option(s) from a dropdown in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Human-readable element description");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Exact target element reference from the page snapshot");
        
        Map<String, Object> valuesProperty = new HashMap<>();
        valuesProperty.put("type", "array");
        valuesProperty.put("description", "Array of values to select in the dropdown");

        Map<String, Object> properties = new HashMap<>();
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);
        properties.put("values", valuesProperty);

        List<String> required = Arrays.asList("element", "ref", "values");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to select option");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_select_option(String element, String ref, String[] values) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.selectOption(element, ref, values);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser select option tool");
        } catch (Exception e) {
            String errorMsg = "Browser Select Option Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

}
