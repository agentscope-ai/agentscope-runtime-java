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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.engine.agents.saa.SandboxAwareTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.browser.TakeScreenshotTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class BrowserScreenshotTaker implements SandboxAwareTool<BrowserScreenshotTaker.Request, BrowserScreenshotTaker.Response> {
	Logger logger = Logger.getLogger(BrowserScreenshotTaker.class.getName());
	private TakeScreenshotTool takeScreenshotTool;

	public BrowserScreenshotTaker() {
		this.takeScreenshotTool = new TakeScreenshotTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = takeScreenshotTool.browser_take_screenshot(request.raw, request.filename, request.element, request.ref, userID, sessionID);
		return new Response(result, "Browser take_screenshot completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return takeScreenshotTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.takeScreenshotTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.takeScreenshotTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.takeScreenshotTool.setSandbox(sandbox);
	}

	public record Request(
			@JsonProperty("raw") Boolean raw,
			@JsonProperty("filename") String filename,
			@JsonProperty("element") String element,
			@JsonProperty("ref") String ref
	) {
		public Request {
			if (raw == null) {
				raw = false;
			}
			if (filename == null) {
				filename = "";
			}
			if (element == null) {
				element = "";
			}
			if (ref == null) {
				ref = "";
			}
		}
	}

	@JsonClassDescription("The result contains browser tool output and message")
	public record Response(String result, String message) {}

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(takeScreenshotTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						takeScreenshotTool.getName(),
						new BrowserScreenshotTaker()
				).description(takeScreenshotTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(BrowserScreenshotTaker.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
