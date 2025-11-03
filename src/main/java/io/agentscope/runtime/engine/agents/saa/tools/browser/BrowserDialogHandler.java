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
import io.agentscope.runtime.sandbox.tools.browser.HandleDialogTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserDialogHandler implements SandboxAwareTool<BrowserDialogHandler.Request, BrowserDialogHandler.Response> {
	Logger logger = Logger.getLogger(BrowserDialogHandler.class.getName());
	private HandleDialogTool handleDialogTool;

	public BrowserDialogHandler() {
		this.handleDialogTool = new HandleDialogTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = handleDialogTool.browser_handle_dialog(request.accept, request.promptText, userID, sessionID);
		return new Response(result, "Browser handle dialog completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return handleDialogTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.handleDialogTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.handleDialogTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.handleDialogTool.setSandbox(sandbox);
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

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(handleDialogTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						handleDialogTool.getName(),
						new BrowserDialogHandler()
				).description(handleDialogTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(BrowserDialogHandler.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
