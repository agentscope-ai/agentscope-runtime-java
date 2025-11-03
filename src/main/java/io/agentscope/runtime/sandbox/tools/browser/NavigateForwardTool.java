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
import java.util.logging.Logger;

/**
 * Browser forward navigation tool
 */
public class NavigateForwardTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(NavigateForwardTool.class.getName());

    public NavigateForwardTool() {
        super("browser_navigate_forward", "browser", "Navigate forward in browser history");
        schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("description", "Request object to navigate forward");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_navigate_forward(String userID, String sessionID) {
        try {
            if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                return browserSandbox.navigateForward();
            }
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.navigateForward();
        } catch (Exception e) {
            String errorMsg = "Browser Navigate Forward Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

}
