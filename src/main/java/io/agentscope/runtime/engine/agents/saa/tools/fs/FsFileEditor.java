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
import io.agentscope.runtime.engine.agents.saa.BaseSandboxAwareTool;
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.sandbox.tools.fs.EditFileTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileEditor extends BaseSandboxAwareTool<EditFileTool, FsFileEditor.EditFileToolRequest, FsFileEditor.EditFileToolResponse> {
	Logger logger = Logger.getLogger(FsFileEditor.class.getName());

	public FsFileEditor() {
		super(new EditFileTool());
	}

	@Override
	public EditFileToolResponse apply(EditFileToolRequest request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = sandboxTool.fs_edit_file(request.path, request.edits, userID, sessionID);
		return new EditFileToolResponse(new Response(result, "Filesystem edit_file completed"));
	}

	public record EditFileToolRequest(
			@JsonProperty(required = true, value = "path")
			@JsonPropertyDescription("Path to the file to edit")
			String path,
			@JsonProperty(required = true, value = "edits")
			@JsonPropertyDescription("Array of edit objects with oldText and newText properties")
			Map<String, Object>[] edits
	) {
		public EditFileToolRequest(String path, Map<String, Object>[] edits) {
			this.path = path;
			this.edits = edits;
		}
	}

	public record EditFileToolResponse(@JsonProperty("Response") Response output) {
		public EditFileToolResponse(Response output) {
			this.output = output;
		}
	}

	@JsonClassDescription("The result contains filesystem tool output and execution message")
	public record Response(String result, String message) {
		public Response(String result, String message) {
			this.result = result;
			this.message = message;
		}

		@JsonProperty(required = true, value = "result")
		@JsonPropertyDescription("tool output")
		public String result() {
			return this.result;
		}

		@JsonProperty(required = true, value = "message")
		@JsonPropertyDescription("execute result")
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
				).inputType(FsFileEditor.EditFileToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
