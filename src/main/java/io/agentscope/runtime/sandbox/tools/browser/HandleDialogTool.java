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

/**
 * Browser dialog handling tool
 */
public class HandleDialogTool extends SandboxTool {

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
                        new DialogHandler()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(DialogHandler.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
    class DialogHandler implements BiFunction<DialogHandler.Request, ToolContext, DialogHandler.Response> {

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DialogHandler.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_handle_dialog(request.accept, request.promptText, userID, sessionID);
            return new Response(result, "Browser handle dialog completed");
        }

        private String browser_handle_dialog(Boolean accept, String promptText, String userID, String sessionID) {
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

        public record Request(
                @JsonProperty(required = true, value = "accept")
                @JsonPropertyDescription("Whether to accept the dialog")
                Boolean accept,
                @JsonProperty(value = "promptText")
                @JsonPropertyDescription("The text of the prompt in case of a prompt dialog")
                String promptText
        ) { 
            public Request {
                if (accept == null) {
                    accept = true;
                }
                if (promptText == null) {
                    promptText = "";
                }
            }
        }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}
