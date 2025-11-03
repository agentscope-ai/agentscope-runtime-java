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
package io.agentscope.runtime.engine.agents.saa.tools.fs;

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
import io.agentscope.runtime.sandbox.tools.fs.ReadFileTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileReader implements SandboxAwareTool<FsFileReader.Request, FsFileReader.Response> {
	Logger logger = Logger.getLogger(FsFileReader.class.getName());
	private ReadFileTool readFileTool;

	public FsFileReader() {
		this.readFileTool = new ReadFileTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = readFileTool.fs_read_file(request.path, userID, sessionID);
		return new Response(result, "Filesystem read_file completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return readFileTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.readFileTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.readFileTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.readFileTool.setSandbox(sandbox);
	}

	public record Request(
			@JsonProperty(required = true, value = "path")
			@JsonPropertyDescription("Path to the file to read")
			String path
	) { }

	@JsonClassDescription("The result contains filesystem tool output and execution message")
	public record Response(String result, String message) {
		public Response(String result, String message) {
			this.result = result;
			this.message = message;
		}

		@JsonProperty(required = true, value = "result")
		@JsonPropertyDescription("tool output")
		public String result() { return this.result; }

		@JsonProperty(required = true, value = "message")
		@JsonPropertyDescription("execute result")
		public String message() { return this.message; }
	}

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(readFileTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						readFileTool.getName(),
						new FsFileReader()
				).description(readFileTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(FsFileReader.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
