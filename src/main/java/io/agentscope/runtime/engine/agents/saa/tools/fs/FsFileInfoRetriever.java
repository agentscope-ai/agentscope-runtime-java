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
import io.agentscope.runtime.sandbox.tools.fs.GetFileInfoTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileInfoRetriever extends BaseSandboxAwareTool<GetFileInfoTool, FsFileInfoRetriever.GetFileInfoToolRequest, FsFileInfoRetriever.GetFileInfoToolResponse> {
	Logger logger = Logger.getLogger(FsFileInfoRetriever.class.getName());

	public FsFileInfoRetriever() {
		super(new GetFileInfoTool());
	}

	@Override
	public GetFileInfoToolResponse apply(GetFileInfoToolRequest request, ToolContext toolContext) {
		String result = sandboxTool.fs_get_file_info(request.path);
		return new GetFileInfoToolResponse(new Response(result, "Filesystem get_file_info completed"));
	}

	public record GetFileInfoToolRequest(
			@JsonProperty(required = true, value = "path")
			@JsonPropertyDescription("Path to the file or directory")
			String path
	) {
		public GetFileInfoToolRequest(String path) {
			this.path = path;
		}
	}

	public record GetFileInfoToolResponse(@JsonProperty("Response") Response output) {
		public GetFileInfoToolResponse(Response output) {
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
				).inputType(FsFileInfoRetriever.GetFileInfoToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
