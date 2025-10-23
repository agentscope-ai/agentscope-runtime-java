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

public class TypeTool extends SandboxTool {

    public TypeTool() {
        super("browser_type", "browser", "Type text into an element in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> elementProperty = new HashMap<>();
        elementProperty.put("type", "string");
        elementProperty.put("description", "Human-readable element description");
        
        Map<String, Object> refProperty = new HashMap<>();
        refProperty.put("type", "string");
        refProperty.put("description", "Exact target element reference");
        
        Map<String, Object> textProperty = new HashMap<>();
        textProperty.put("type", "string");
        textProperty.put("description", "Text to type");
        
        Map<String, Object> submitProperty = new HashMap<>();
        submitProperty.put("type", "boolean");
        submitProperty.put("description", "Whether to submit after typing");
        
        Map<String, Object> slowlyProperty = new HashMap<>();
        slowlyProperty.put("type", "boolean");
        slowlyProperty.put("description", "Whether to type slowly");

        Map<String, Object> properties = new HashMap<>();
        properties.put("element", elementProperty);
        properties.put("ref", refProperty);
        properties.put("text", textProperty);
        properties.put("submit", submitProperty);
        properties.put("slowly", slowlyProperty);

        List<String> required = Arrays.asList("element", "ref", "text");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to type text");
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
                        new TypeExecutor()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(TypeExecutor.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
    class TypeExecutor implements BiFunction<TypeExecutor.Request, ToolContext, TypeExecutor.Response> {

        Logger logger = Logger.getLogger(TypeExecutor.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_type(request.element, request.ref, request.text, request.submit, request.slowly, userID, sessionID);
            return new Response(result, "Browser type completed");
        }

        private String browser_type(String element, String ref, String text, Boolean submit, Boolean slowly, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.type(element, ref, text, submit, slowly);
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.type(element, ref, text, submit, slowly);
            } catch (Exception e) {
                String errorMsg = "Browser Type Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty(required = true, value = "element") String element,
                @JsonProperty(required = true, value = "ref") String ref,
                @JsonProperty(required = true, value = "text") String text,
                @JsonProperty("submit") Boolean submit,
                @JsonProperty("slowly") Boolean slowly
        ) { 
            public Request {
                if (submit == null) {
                    submit = false;
                }
                if (slowly == null) {
                    slowly = false;
                }
            }
        }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}
