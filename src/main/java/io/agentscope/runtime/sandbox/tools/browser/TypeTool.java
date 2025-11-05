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

public class TypeTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(TypeTool.class.getName());

    public TypeTool() {
        super("browser_type", "browser", "Type text into an element in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Human-readable element description");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Exact target element reference");
        
        Map<String, Object> textProperty = new HashMap<>();
        textProperty.put("type", "string");
        textProperty.put("description", "Text to type");
        
        Map<String, Object> submitProperty = new HashMap<>();
        submitProperty.put("type", "boolean");
        submitProperty.put("description", "Whether to submit after typing");
        
        Map<String, Object> slowlyProperty = new HashMap<>();
        slowlyProperty.put("type", "boolean");
        slowlyProperty.put("description", "Whether to type slowly");

        Map<String, Object> properties = new HashMap<>();
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);
        properties.put("text", textProperty);
        properties.put("submit", submitProperty);
        properties.put("slowly", slowlyProperty);

        List<String> required = Arrays.asList("element", "ref", "text");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to type text");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_type(String element, String ref, String text, Boolean submit, Boolean slowly) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.type(element, ref, text, submit, slowly);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser type tool");
        } catch (Exception e) {
            String errorMsg = "Browser Type Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

}
