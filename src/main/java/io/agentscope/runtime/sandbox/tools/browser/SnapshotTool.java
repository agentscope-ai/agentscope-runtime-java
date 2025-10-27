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

public class SnapshotTool extends SandboxTool {

    public SnapshotTool() {
        super("browser_snapshot", "browser", "Take a snapshot of the current browser state");
        schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", new HashMap<>());
        schema.put("description", "Request object to take snapshot");
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
                        new SnapshotTaker()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(SnapshotTaker.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }
    class SnapshotTaker implements BiFunction<SnapshotTaker.Request, ToolContext, SnapshotTaker.Response> {

        Logger logger = Logger.getLogger(SnapshotTaker.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_snapshot(userID, sessionID);
            return new Response(result, "Browser snapshot completed");
        }

        private String browser_snapshot(String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.snapshot();
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.snapshot();
            } catch (Exception e) {
                String errorMsg = "Browser Snapshot Error: " + e.getMessage();
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

