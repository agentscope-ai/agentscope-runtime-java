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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class ResizeTool extends SandboxTool {

    public ResizeTool() {
        super("browser_resize", "browser", "Resize the browser window");
        schema = new HashMap<>();
        
        Map<String, Object> widthProperty = new HashMap<>();
        widthProperty.put("type", "number");
        widthProperty.put("description", "Width of the browser window");
        
        Map<String, Object> heightProperty = new HashMap<>();
        heightProperty.put("type", "number");
        heightProperty.put("description", "Height of the browser window");

        Map<String, Object> properties = new HashMap<>();
        properties.put("width", widthProperty);
        properties.put("height", heightProperty);

        List<String> required = Arrays.asList("width", "height");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to resize browser");
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
                        new WindowResizer()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(WindowResizer.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理浏览器窗口调整的工具
     */
    class WindowResizer implements BiFunction<WindowResizer.Request, ToolContext, WindowResizer.Response> {

        Logger logger = Logger.getLogger(WindowResizer.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_resize(request.width, request.height, userID, sessionID);
            return new Response(result, "Browser resize completed");
        }

        private String browser_resize(Double width, Double height, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.resize(width, height);
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.resize(width, height);
            } catch (Exception e) {
                String errorMsg = "Browser Resize Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty(required = true, value = "width")
                @JsonPropertyDescription("Width of the browser window")
                Double width,
                @JsonProperty(required = true, value = "height")
                @JsonPropertyDescription("Height of the browser window")
                Double height
        ) { }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}

