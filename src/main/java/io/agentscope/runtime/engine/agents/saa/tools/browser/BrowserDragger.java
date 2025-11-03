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
import io.agentscope.runtime.sandbox.tools.browser.DragTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserDragger implements SandboxAwareTool<BrowserDragger.Request, BrowserDragger.Response> {
	Logger logger = Logger.getLogger(BrowserDragger.class.getName());
	private DragTool dragTool;

	public BrowserDragger() {
		this.dragTool = new DragTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = dragTool.browser_drag(request.startElement, request.startRef, request.endElement, request.endRef, userID, sessionID);
		return new Response(result, "Browser drag completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return dragTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.dragTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.dragTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.dragTool.setSandbox(sandbox);
	}

	public record Request(
			@JsonProperty(required = true, value = "startElement")
			@JsonPropertyDescription("Human-readable source element description")
			String startElement,
			@JsonProperty(required = true, value = "startRef")
			@JsonPropertyDescription("Exact source element reference from the page snapshot")
			String startRef,
			@JsonProperty(required = true, value = "endElement")
			@JsonPropertyDescription("Human-readable target element description")
			String endElement,
			@JsonProperty(required = true, value = "endRef")
			@JsonPropertyDescription("Exact target element reference from the page snapshot")
			String endRef
	) { }

	@JsonClassDescription("The result contains browser tool output and message")
	public record Response(String result, String message) {}

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(dragTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						dragTool.getName(),
						new BrowserDragger()
				).description(dragTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(BrowserDragger.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
