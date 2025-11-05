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

public class TabNewTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(TabNewTool.class.getName());

    public TabNewTool() {
        super("browser_tab_new", "browser", "Open a new browser tab");
        schema = new HashMap<>();
        
        Map<String, Object> urlProperty = new HashMap<>();
        urlProperty.put("type", "string");
        urlProperty.put("description", "The URL to navigate to in the new tab");

        Map<String, Object> properties = new HashMap<>();
        properties.put("url", urlProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to create new tab");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_tab_new(String url) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.tabNew(url);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser tab new tool");
        } catch (Exception e) {
            String errorMsg = "Browser Tab New Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

}
