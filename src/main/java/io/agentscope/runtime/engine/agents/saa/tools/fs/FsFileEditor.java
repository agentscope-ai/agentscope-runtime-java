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

import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.engine.agents.saa.SandboxAwareTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.fs.EditFileTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileEditor implements SandboxAwareTool<FsFileEditor.Request, FsFileEditor.Response> {
	Logger logger = Logger.getLogger(FsFileEditor.class.getName());
	private EditFileTool editFileTool;

	public FsFileEditor() {
		this.editFileTool = new EditFileTool();
	}

	@Override
	public Response apply(Request request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = editFileTool.fs_edit_file(request.path, request.edits, userID, sessionID);
		return new Response(result, "Filesystem edit_file completed");
	}

	@Override
	public SandboxManager getSandboxManager() {
		return editFileTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.editFileTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.editFileTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.editFileTool.setSandbox(sandbox);
	}

	public record Request(
			@JsonProperty(required = true, value = "path")
			@JsonPropertyDescription("Path to the file to edit")
			String path,
			@JsonProperty(required = true, value = "edits")
			@JsonPropertyDescription("Array of edit objects with oldText and newText properties")
			Map<String, Object>[] edits
	) { }

	@JsonClassDescription("The result contains filesystem tool output and execution message")
	public record Response(String result, String message) {
		public Response(String result, String message) { this.result = result; this.message = message; }
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
			inputSchema = mapper.writeValueAsString(editFileTool.getSchema());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						editFileTool.getName(),
						new FsFileEditor()
				).description(editFileTool.getDescription())
				.inputSchema(
						inputSchema
				).inputType(FsFileEditor.Request.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
