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
import io.agentscope.runtime.sandbox.tools.browser.WaitForTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserWaiter implements SandboxAwareTool<BrowserWaiter.Request, BrowserWaiter.Response> {
	Logger logger = Logger.getLogger(BrowserWaiter.class.getName());
	private WaitForTool waitForTool;

	public BrowserWaiter() {
		this.waitForTool = new WaitForTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = waitForTool.browser_wait_for(request.time, request.text, request.textGone, userID, sessionID);
		return new Response(result, "Browser wait_for completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return waitForTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.waitForTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.waitForTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.waitForTool.setSandbox(sandbox);
	}

	public record Request(
			@JsonProperty("time") @JsonPropertyDescription("time in seconds") Double time,
			@JsonProperty("text") String text,
			@JsonProperty("textGone") String textGone
	) {
		public Request {
			if (time == null) {
				time = 0.0;
			}
			if (text == null) {
				text = "";
			}
			if (textGone == null) {
				textGone = "";
			}
		}
	}

	@JsonClassDescription("The result contains browser tool output and message")
	public record Response(String result, String message) {}

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(waitForTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						waitForTool.getName(),
						new BrowserWaiter()
				).description(waitForTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(BrowserWaiter.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
