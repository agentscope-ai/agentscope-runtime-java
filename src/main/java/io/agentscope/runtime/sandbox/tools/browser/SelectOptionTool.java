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

/**
 * Browser dropdown selection tool
 */
public class SelectOptionTool extends SandboxTool {

    public SelectOptionTool() {
        super("browser_select_option", "browser", "Select option(s) from a dropdown in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Human-readable element description");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Exact target element reference from the page snapshot");
        
        Map<String, Object> valuesProperty = new HashMap<>();
        valuesProperty.put("type", "array");
        valuesProperty.put("description", "Array of values to select in the dropdown");

        Map<String, Object> properties = new HashMap<>();
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);
        properties.put("values", valuesProperty);

        List<String> required = Arrays.asList("element", "ref", "values");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to select option");
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
                        new OptionSelector()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(OptionSelector.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理下拉选项选择的工具
     */
    class OptionSelector implements BiFunction<OptionSelector.Request, ToolContext, OptionSelector.Response> {

        Logger logger = Logger.getLogger(OptionSelector.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_select_option(request.element, request.ref, request.values, userID, sessionID);
            return new Response(result, "Browser select option completed");
        }

        private String browser_select_option(String element, String ref, String[] values, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.selectOption(element, ref, values);
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.selectOption(element, ref, values);
            } catch (Exception e) {
                String errorMsg = "Browser Select Option Error: " + e.getMessage();
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
                String ref,
                @JsonProperty(required = true, value = "values")
                @JsonPropertyDescription("Array of values to select in the dropdown")
                String[] values
        ) { }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}
