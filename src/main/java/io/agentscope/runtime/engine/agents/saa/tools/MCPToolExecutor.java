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
package io.agentscope.runtime.engine.agents.saa.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.agents.saa.RuntimeFunctionToolCallback;
import io.agentscope.runtime.engine.agents.saa.SandboxAwareTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;

public class MCPToolExecutor implements SandboxAwareTool<Map<String, Object>, MCPToolExecutor.MCPToolResponse> {
	private static final Logger logger = Logger.getLogger(MCPToolExecutor.class.getName());

	private MCPTool mcpTool;

	public MCPToolExecutor(String name, String toolType, String description,
			Map<String, Object> schema, Map<String, Object> serverConfigs,
			SandboxType sandboxType, SandboxManager sandboxManager) {
		this.mcpTool = new MCPTool(
				name,
				toolType,
				description,
				schema,
				serverConfigs,
				sandboxType,
				sandboxManager
		);
	}

	public MCPToolExecutor(MCPTool mcpTool) {
		this.mcpTool = mcpTool;
	}

	@Override
	public MCPToolResponse apply(Map<String, Object> arguments, ToolContext toolContext) {
		String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
		String userID = userAndSession[0];
		String sessionID = userAndSession[1];

		try {
			String result = mcpTool.executeMCPTool(arguments, userID, sessionID);
			return new MCPToolResponse(result, "MCP tool execution completed");
		} catch (Exception e) {
			logger.severe("MCP tool execution error: " + e.getMessage());
			e.printStackTrace();
			return new MCPToolResponse(
					"Error",
					"MCP tool execution error: " + e.getMessage()
			);
		}
	}

	@Override
	public SandboxManager getSandboxManager() {
		return mcpTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		mcpTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return mcpTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		mcpTool.setSandbox(sandbox);
	}

	public record MCPToolResponse(String result, String message) {
		public MCPToolResponse(String result, String message) {
			this.result = result;
			this.message = message;
		}
	}

	public RuntimeFunctionToolCallback buildTool() {
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = "";
		try {
			inputSchema = mapper.writeValueAsString(mcpTool.getSchema());
		} catch (Exception e) {
			logger.severe("Failed to serialize schema: " + e.getMessage());
			e.printStackTrace();
		}

		return RuntimeFunctionToolCallback
				.builder(
						mcpTool.getName(),
						this
				)
				.description(mcpTool.getDescription())
				.inputSchema(inputSchema)
				.inputType(Map.class)
				.toolMetadata(ToolMetadata.builder().returnDirect(false).build())
				.build();
	}

}
