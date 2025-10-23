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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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

public class TakeScreenshotTool extends SandboxTool {

    public TakeScreenshotTool() {
        super("browser_take_screenshot", "browser", "Take a screenshot of the browser");
        schema = new HashMap<>();
        
        Map<String, Object> rawProperty = new HashMap<>();
        rawProperty.put("type", "boolean");
        rawProperty.put("description", "Whether to return raw image data");
        
        Map<String, Object> filenameProperty = new HashMap<>();
        filenameProperty.put("type", "string");
        filenameProperty.put("description", "Filename to save screenshot");
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Element description for partial screenshot");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Element reference for partial screenshot");

        Map<String, Object> properties = new HashMap<>();
        properties.put("raw", rawProperty);
        properties.put("filename", filenameProperty);
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to take screenshot");
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
                        new ScreenshotTaker()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(ScreenshotTaker.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理浏览器截图的工具
     */
    class ScreenshotTaker implements BiFunction<ScreenshotTaker.Request, ToolContext, ScreenshotTaker.Response> {

        Logger logger = Logger.getLogger(ScreenshotTaker.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_take_screenshot(request.raw, request.filename, request.element, request.ref, userID, sessionID);
            return new Response(result, "Browser take_screenshot completed");
        }

        private String browser_take_screenshot(Boolean raw, String filename, String element, String ref, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.takeScreenshot(raw, filename, element, ref);
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.takeScreenshot(raw, filename, element, ref);
            } catch (Exception e) {
                String errorMsg = "Browser Take Screenshot Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty("raw") Boolean raw,
                @JsonProperty("filename") String filename,
                @JsonProperty("element") String element,
                @JsonProperty("ref") String ref
        ) { 
            public Request {
                if (raw == null) {
                    raw = false;
                }
                if (filename == null) {
                    filename = "";
                }
                if (element == null) {
                    element = "";
                }
                if (ref == null) {
                    ref = "";
                }
            }
        }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}
