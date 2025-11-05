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
import io.agentscope.runtime.sandbox.tools.fs.ReadFileTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class FsFileReader extends BaseSandboxAwareTool<ReadFileTool, FsFileReader.ReadFileToolRequest, FsFileReader.ReadFileToolResponse> {
	Logger logger = Logger.getLogger(FsFileReader.class.getName());

	public FsFileReader() {
		super(new ReadFileTool());
	}

	@Override
	public ReadFileToolResponse apply(ReadFileToolRequest request, ToolContext toolContext) {
		String result = sandboxTool.fs_read_file(request.path);
		return new ReadFileToolResponse(new Response(result, "Filesystem read_file completed"));
	}

	public record ReadFileToolRequest(
			@JsonProperty(required = true, value = "path")
			@JsonPropertyDescription("Path to the file to read")
			String path
	) {
		public ReadFileToolRequest(String path) {
			this.path = path;
		}
	}

	public record ReadFileToolResponse(@JsonProperty("Response") Response output) {
		public ReadFileToolResponse(Response output) {
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
				).inputType(FsFileReader.ReadFileToolRequest.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}
}
