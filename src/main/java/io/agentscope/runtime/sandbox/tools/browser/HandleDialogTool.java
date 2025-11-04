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
 * Browser dialog handling tool
 */
public class HandleDialogTool extends BrowserSandboxTool {

    Logger logger = Logger.getLogger(HandleDialogTool.class.getName());

    public HandleDialogTool() {
        super("browser_handle_dialog", "browser", "Handle browser dialogs (alert, confirm, prompt)");
        schema = new HashMap<>();
        
        Map<String, Object> acceptProperty = new HashMap<>();
        acceptProperty.put("type", "boolean");
        acceptProperty.put("description", "Whether to accept the dialog");
        
        Map<String, Object> promptTextProperty = new HashMap<>();
        promptTextProperty.put("type", "string");
        promptTextProperty.put("description", "The text of the prompt in case of a prompt dialog");

        Map<String, Object> properties = new HashMap<>();
        properties.put("accept", acceptProperty);
        properties.put("promptText", promptTextProperty);

        List<String> required = Arrays.asList("accept");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to handle dialog");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_handle_dialog(Boolean accept, String promptText, String userID, String sessionID) {
        try {
            if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                return browserSandbox.handleDialog(accept, promptText);
            }
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.handleDialog(accept, promptText);
        } catch (Exception e) {
            String errorMsg = "Browser Handle Dialog Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
