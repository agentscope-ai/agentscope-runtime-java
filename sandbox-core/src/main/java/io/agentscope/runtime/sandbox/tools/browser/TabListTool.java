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

/**
 * Browser tab list tool
 */
public class TabListTool extends BrowserSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(TabListTool.class);

    public TabListTool() {
        super("browser_tab_list", "browser", "List all browser tabs");
        schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("description", "Request object to list tabs");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_tab_list() {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.tabList();
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser tab list tool");
        } catch (Exception e) {
            String errorMsg = "Browser Tab List Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }

}
