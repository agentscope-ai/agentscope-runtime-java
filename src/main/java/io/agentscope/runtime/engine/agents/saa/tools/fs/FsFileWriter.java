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
import io.agentscope.runtime.engine.agents.saa.BaseSandboxAwareTool;
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.sandbox.tools.fs.WriteFileTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileWriter extends BaseSandboxAwareTool<WriteFileTool, FsFileWriter.WriteFileToolRequest, FsFileWriter.WriteFileToolResponse> {
	Logger logger = Logger.getLogger(FsFileWriter.class.getName());

	public FsFileWriter() {
		super(new WriteFileTool());
	}

	@Override
	public WriteFileToolResponse apply(WriteFileToolRequest request, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		String result = sandboxTool.fs_write_file(request.path, request.content, userID, sessionID);
		return new WriteFileToolResponse(new Response(result, "Filesystem write_file completed"));
	}

	public record WriteFileToolRequest(
			@JsonProperty(required = true, value = "path")
			@JsonPropertyDescription("Path to the file to write to")
			String path,
			@JsonProperty(required = true, value = "content")
			@JsonPropertyDescription("Content to write into the file")
			String content
	) {
		public WriteFileToolRequest(String path, String content) {
			this.path = path;
			this.content = content;
		}
	}

	public record WriteFileToolResponse(@JsonProperty("Response") Response output) {
		public WriteFileToolResponse(Response output) {
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
				).inputType(FsFileWriter.WriteFileToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
