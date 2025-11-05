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
import io.agentscope.runtime.sandbox.tools.fs.MoveFileTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileMover extends BaseSandboxAwareTool<MoveFileTool, FsFileMover.MoveFileToolRequest, FsFileMover.MoveFileToolResponse> {
	Logger logger = Logger.getLogger(FsFileMover.class.getName());

	public FsFileMover() {
		super(new MoveFileTool());
	}

	@Override
	public MoveFileToolResponse apply(MoveFileToolRequest request, ToolContext toolContext) {
		String result = sandboxTool.fs_move_file(request.source, request.destination);
		return new MoveFileToolResponse(new Response(result, "Filesystem move_file completed"));
	}

	public record MoveFileToolRequest(
			@JsonProperty(required = true, value = "source")
			@JsonPropertyDescription("Source path to move from")
			String source,
			@JsonProperty(required = true, value = "destination")
			@JsonPropertyDescription("Destination path to move to")
			String destination
	) {
		public MoveFileToolRequest(String source, String destination) {
			this.source = source;
			this.destination = destination;
		}
	}

	public record MoveFileToolResponse(@JsonProperty("Response") Response output) {
		public MoveFileToolResponse(Response output) {
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
				).inputType(FsFileMover.MoveFileToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
