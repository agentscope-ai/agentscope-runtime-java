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
 * Browser key press tool
 */
public class PressKeyTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(PressKeyTool.class.getName());

    public PressKeyTool() {
        super("browser_press_key", "browser", "Press a key in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> keyProperty = new HashMap<>();
        keyProperty.put("type", "string");
        keyProperty.put("description", "Name of the key to press or a character to generate");

        Map<String, Object> properties = new HashMap<>();
        properties.put("key", keyProperty);

        List<String> required = Arrays.asList("key");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to press key");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_press_key(String key, String userID, String sessionID) {
        try {
            if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                return browserSandbox.pressKey(key);
            }
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.pressKey(key);
        } catch (Exception e) {
            String errorMsg = "Browser Press Key Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
