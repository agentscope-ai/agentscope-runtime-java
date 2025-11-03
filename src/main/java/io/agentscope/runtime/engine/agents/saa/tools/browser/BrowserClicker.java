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
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.engine.agents.saa.SandboxAwareTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.browser.ClickTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserClicker implements SandboxAwareTool<BrowserClicker.Request, BrowserClicker.Response> {
	Logger logger = Logger.getLogger(BrowserClicker.class.getName());
	private ClickTool clickTool;

	public BrowserClicker() {
		this.clickTool = new ClickTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = clickTool.browser_click(request.element, request.ref, userID, sessionID);
		return new Response(result, "Browser click completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return clickTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.clickTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.clickTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.clickTool.setSandbox(sandbox);
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

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(clickTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						clickTool.getName(),
						new BrowserClicker()
				).description(clickTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(BrowserClicker.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
