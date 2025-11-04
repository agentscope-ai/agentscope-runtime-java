/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.engine.agents.saa.tools.browser;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.agents.saa.BaseSandboxAwareTool;
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.sandbox.tools.browser.PdfSaveTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserPdfSaver extends BaseSandboxAwareTool<PdfSaveTool, BrowserPdfSaver.PdfSaveToolRequest, BrowserPdfSaver.PdfSaveToolResponse> {
	Logger logger = Logger.getLogger(BrowserPdfSaver.class.getName());

	public BrowserPdfSaver() {
		super(new PdfSaveTool());
	}

	@Override
	public PdfSaveToolResponse apply(PdfSaveToolRequest request, ToolContext toolContext) {
		String result = sandboxTool.browser_pdf_save(request.filename);
		return new PdfSaveToolResponse(new Response(result, "Browser PDF save completed"));
	}

	public record PdfSaveToolRequest(
			@JsonProperty("filename")
			@JsonPropertyDescription("File name to save the pdf to")
			String filename
	) {
		public PdfSaveToolRequest {
			if (filename == null) {
				filename = "";
			}
		}
	}

	public record PdfSaveToolResponse(@JsonProperty("Response") Response output) {
		public PdfSaveToolResponse(Response output) {
			this.output = output;
		}
	}

	@JsonClassDescription("The result contains browser tool output and message")
	public record Response(String result, String message) {
		public Response(String result, String message) {
			this.result = result;
			this.message = message;
		}

		@JsonProperty(required = true, value = "result")
		public String result() {
			return this.result;
		}

		@JsonProperty(required = true, value = "message")
		public String message() {
			return this.message;
		}
	}

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(sandboxTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						sandboxTool.getName(),
						this
				).description(sandboxTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(BrowserPdfSaver.PdfSaveToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
