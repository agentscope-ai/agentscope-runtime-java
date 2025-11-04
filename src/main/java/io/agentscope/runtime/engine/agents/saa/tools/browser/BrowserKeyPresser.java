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
import io.agentscope.runtime.sandbox.tools.browser.PressKeyTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserKeyPresser extends BaseSandboxAwareTool<PressKeyTool, BrowserKeyPresser.PressKeyToolRequest, BrowserKeyPresser.PressKeyToolResponse> {
	Logger logger = Logger.getLogger(BrowserKeyPresser.class.getName());

	public BrowserKeyPresser() {
		super(new PressKeyTool());
	}

	@Override
	public PressKeyToolResponse apply(PressKeyToolRequest request, ToolContext toolContext) {
		String result = sandboxTool.browser_press_key(request.key);
		return new PressKeyToolResponse(new Response(result, "Browser press key completed"));
	}

	public record PressKeyToolRequest(
			@JsonProperty(required = true, value = "key")
			@JsonPropertyDescription("Name of the key to press or a character to generate")
			String key
	) {
		public PressKeyToolRequest(String key) {
			this.key = key;
		}
	}

	public record PressKeyToolResponse(@JsonProperty("Response") Response output) {
		public PressKeyToolResponse(Response output) {
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
				).inputType(BrowserKeyPresser.PressKeyToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
