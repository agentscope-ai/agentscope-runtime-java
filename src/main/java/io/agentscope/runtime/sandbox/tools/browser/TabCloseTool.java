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

public class TabCloseTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(TabCloseTool.class.getName());

    public TabCloseTool() {
        super("browser_tab_close", "browser", "Close a browser tab");
        schema = new HashMap<>();
        
        Map<String, Object> indexProperty = new HashMap<>();
        indexProperty.put("type", "integer");
        indexProperty.put("description", "The index of the tab to close");

        Map<String, Object> properties = new HashMap<>();
        properties.put("index", indexProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to close tab");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_tab_close(Integer index) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.tabClose(index);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser tab close tool");
        } catch (Exception e) {
            String errorMsg = "Browser Tab Close Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

}
