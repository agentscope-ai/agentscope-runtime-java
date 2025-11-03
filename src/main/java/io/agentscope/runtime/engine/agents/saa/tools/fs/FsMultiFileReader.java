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
import io.agentscope.runtime.sandbox.tools.fs.ReadMultipleFilesTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsMultiFileReader implements SandboxAwareTool<FsMultiFileReader.Request, FsMultiFileReader.Response> {
	Logger logger = Logger.getLogger(FsMultiFileReader.class.getName());
	private ReadMultipleFilesTool readMultipleFilesTool;

	public FsMultiFileReader() {
		this.readMultipleFilesTool = new ReadMultipleFilesTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = readMultipleFilesTool.fs_read_multiple_files(request.paths, userID, sessionID);
		return new Response(result, "Filesystem read_multiple_files completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return readMultipleFilesTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.readMultipleFilesTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.readMultipleFilesTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.readMultipleFilesTool.setSandbox(sandbox);
	}

	public record Request(
			@JsonProperty(required = true, value = "paths")
			@JsonPropertyDescription("Paths to the files to read")
			String[] paths
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
			inputSchema = mapper.writeValueAsString(readMultipleFilesTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						readMultipleFilesTool.getName(),
						new FsMultiFileReader()
				).description(readMultipleFilesTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(FsMultiFileReader.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
