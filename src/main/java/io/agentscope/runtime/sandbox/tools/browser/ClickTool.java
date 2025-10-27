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

public class ClickTool extends SandboxTool {

    public ClickTool() {
        super("browser_click", "browser", "Click on an element in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Human-readable element description");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Exact target element reference from the page snapshot");

        Map<String, Object> properties = new HashMap<>();
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);

        List<String> required = Arrays.asList("element", "ref");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to click element");
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
                        new ClickExecutor()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(ClickExecutor.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
    class ClickExecutor implements BiFunction<ClickExecutor.Request, ToolContext, ClickExecutor.Response> {

        Logger logger = Logger.getLogger(ClickExecutor.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_click(request.element, request.ref, userID, sessionID);
            return new Response(result, "Browser click completed");
        }

        private String browser_click(String element, String ref, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.click(element, ref);
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.click(element, ref);
            } catch (Exception e) {
                String errorMsg = "Browser Click Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty(required = true, value = "element")
                @JsonPropertyDescription("Human-readable element description")
                String element,
                @JsonProperty(required = true, value = "ref")
                @JsonPropertyDescription("Exact target element reference from the page snapshot")
                String ref
        ) { }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}

