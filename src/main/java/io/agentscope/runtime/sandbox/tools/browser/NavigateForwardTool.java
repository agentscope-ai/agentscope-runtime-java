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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Browser forward navigation tool
 */
public class NavigateForwardTool extends SandboxTool {

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

    @Override
    public ToolCallback buildTool() {
        ObjectMapper mapper = new ObjectMapper();
        String inputSchema = "";
        try {
            inputSchema = mapper.writeValueAsString(schema);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return FunctionToolCallback
                .builder(
                        name,
                        new ForwardNavigator()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(ForwardNavigator.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理浏览器前进的工具
     */
    class ForwardNavigator implements BiFunction<ForwardNavigator.Request, ToolContext, ForwardNavigator.Response> {

        Logger logger = Logger.getLogger(ForwardNavigator.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_navigate_forward(userID, sessionID);
            return new Response(result, "Browser navigate forward completed");
        }

        private String browser_navigate_forward(String userID, String sessionID) {
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

        public record Request() { }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}
