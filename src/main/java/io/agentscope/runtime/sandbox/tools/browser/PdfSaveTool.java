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

/**
 * Browser PDF save tool
 */
public class PdfSaveTool extends SandboxTool {

    public PdfSaveTool() {
        super("browser_pdf_save", "browser", "Save the current page as PDF");
        schema = new HashMap<>();
        
        Map<String, Object> filenameProperty = new HashMap<>();
        filenameProperty.put("type", "string");
        filenameProperty.put("description", "File name to save the pdf to");

        Map<String, Object> properties = new HashMap<>();
        properties.put("filename", filenameProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to save PDF");
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
                        new PdfSaver()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(PdfSaver.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理PDF保存的工具
     */
    class PdfSaver implements BiFunction<PdfSaver.Request, ToolContext, PdfSaver.Response> {

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(PdfSaver.class.getName());

        @Override
        public Response apply(Request request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];
            
            String result = browser_pdf_save(request.filename, userID, sessionID);
            return new Response(result, "Browser PDF save completed");
        }

        private String browser_pdf_save(String filename, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BrowserSandbox browserSandbox) {
                    return browserSandbox.pdfSave(filename);
                }
                BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
                return browserSandbox.pdfSave(filename);
            } catch (Exception e) {
                String errorMsg = "Browser PDF Save Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }

        public record Request(
                @JsonProperty("filename")
                @JsonPropertyDescription("File name to save the pdf to")
                String filename
        ) { 
            public Request {
                if (filename == null) {
                    filename = "";
                }
            }
        }

        @JsonClassDescription("The result contains browser tool output and message")
        public record Response(String result, String message) {}
    }
}
