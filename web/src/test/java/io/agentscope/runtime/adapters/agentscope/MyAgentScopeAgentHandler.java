/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.adapters.agentscope;

import java.util.List;
import java.util.Map;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Test implementation of AgentScopeAgentAdapter.
 *
 * <p>This implementation follows the following logic:
 * <ul>
 *   <li>Exports state from StateService</li>
 *   <li>Creates ReActAgent with MemoryAdapter and Toolkit</li>
 *   <li>Loads state if available</li>
 *   <li>Streams agent responses</li>
 *   <li>Saves state after completion</li>
 * </ul>
 */
public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {
	private static final Logger logger = LoggerFactory.getLogger(MyAgentScopeAgentHandler.class);
	private final String apiKey;

	/**
	 * Creates a new TestAgentScopeAgentAdapter.
	 */
	public MyAgentScopeAgentHandler() {
		apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
	}

	@Override
	public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
		String sessionId = request.getSessionId();
		String userId = request.getUserId();

		try {
			// Step 1: Export state from StateService
			Map<String, Object> state = null;
			if (stateService != null) {
				try {
					state = stateService.exportState(userId, sessionId, null).join();
				}
				catch (Exception e) {
					logger.warn("Failed to export state: {}", e.getMessage());
				}
			}

			// Step 2: Create Toolkit and register tools
			Toolkit toolkit = new Toolkit();
			if (sandboxService != null) {
				try {
					// Create a BaseSandbox instance for this session
                    Sandbox sandbox = new BaseSandbox(sandboxService, userId, sessionId);

					// Register Python code execution tool (matching Python: execute_python_code)
					toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
					logger.debug("Registered execute_python_code tool");
				}
				catch (Exception e) {
					logger.warn("Failed to create sandbox or register tools: {}", e.getMessage());
					// Continue without tools if sandbox creation fails
				}
			}

			// Step 3: Create MemoryAdapter
			MemoryAdapter memory = null;
			if (sessionHistoryService != null) {
				memory = new MemoryAdapter(
						sessionHistoryService,
						userId,
						sessionId
				);
			}

			// Step 4: Create LongTermMemoryAdapter
			LongTermMemoryAdapter longTermMemory = null;
			if (memoryService != null) {
				longTermMemory = new LongTermMemoryAdapter(
						memoryService,
						userId,
						sessionId
				);
			}

			// Step 5: Create ReActAgent
			ReActAgent.Builder agentBuilder = ReActAgent.builder()
					.name("Friday")
					.sysPrompt("You're a helpful assistant named Friday.")
					.toolkit(toolkit)
					.model(
							DashScopeChatModel.builder()
									.apiKey(apiKey)
									.modelName("qwen-turbo")
									.enableThinking(true)
									.stream(true)
									.formatter(new DashScopeChatFormatter())
									.build());

			// Add long-term memory if available
			if (longTermMemory != null) {
				agentBuilder.longTermMemory(longTermMemory)
						.longTermMemoryMode(LongTermMemoryMode.BOTH);
				logger.debug("Long-term memory configured");
			}

			if (memory != null) {
				agentBuilder.memory(memory);
				logger.debug("Memory adapter configured");
			}

			ReActAgent agent = agentBuilder.build();

			// Step 6: Load state if available
			if (state != null && !state.isEmpty()) {
				try {
					agent.loadStateDict(state);
					logger.debug("Loaded state for session: {}", sessionId);
				}
				catch (Exception e) {
					logger.warn("Failed to load state: {}", e.getMessage());
				}
			}

			// Step 7: Convert messages parameter to List<Msg>
			// Python version: msgs parameter is already a list of Msg
			List<Msg> agentMessages;
			if (messages instanceof List) {
				@SuppressWarnings("unchecked")
				List<Msg> msgList = (List<Msg>) messages;
				agentMessages = msgList;
			}
			else if (messages instanceof Msg) {
				agentMessages = List.of((Msg) messages);
			}
			else {
				logger.warn("Unexpected messages type: {}, using empty list",
						messages != null ? messages.getClass().getName() : "null");
				agentMessages = List.of();
			}

			// Step 8: Stream agent responses (matching Python: async for msg, last in stream_printing_messages(...))
			// Configure streaming options - match Python version's streaming behavior
			StreamOptions streamOptions = StreamOptions.builder()
					.eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
					.incremental(true)
					.build();

			// Python version: agent(msgs) - passes the entire list
			// In Java, ReActAgent.stream() accepts a single Msg, so we use the first message
			// or combine all messages into memory first, then call with the last message
			Msg queryMessage;
			if (agentMessages.isEmpty()) {
				// No messages provided, create a default empty message
				queryMessage = Msg.builder()
						.role(io.agentscope.core.message.MsgRole.USER)
						.build();
			}
			else if (agentMessages.size() == 1) {
				// Single message - use it directly
				queryMessage = agentMessages.get(0);
			}
			else {
				// Multiple messages - add all but the last to memory, then use the last one for query
				// This matches Python behavior where all messages are in memory and the last is the query
				for (int i = 0; i < agentMessages.size() - 1; i++) {
					agent.getMemory().addMessage(agentMessages.get(i));
				}
				queryMessage = agentMessages.get(agentMessages.size() - 1);
			}

			// Stream agent responses (matching Python: async for msg, last in stream_printing_messages(...))
			Flux<io.agentscope.core.agent.Event> agentScopeEvents = agent.stream(queryMessage, streamOptions);

			// Return raw framework stream - Runner will handle conversion via StreamAdapter
			// Note: Runner calls streamAdapter.adaptFrameworkStream() on the returned stream,
			// so we should return the raw AgentScope Event stream, not the converted runtime Event stream
			return agentScopeEvents
					.doFinally(signalType -> {
						// Step 10: Save state after completion
						if (stateService != null) {
							try {
								Map<String, Object> finalState = agent.stateDict();
								if (finalState != null && !finalState.isEmpty()) {
									stateService.saveState(userId, finalState, sessionId, null)
											.exceptionally(e -> {
												logger.error("Failed to save state: {}", e.getMessage(), e);
												return null;
											});
								}
							}
							catch (Exception e) {
								logger.error("Error saving state: {}", e.getMessage(), e);
							}
						}
					})
					.doOnError(error -> {
						logger.error("Error in agent stream: {}", error.getMessage(), error);
					});

		}
		catch (Exception e) {
			logger.error("Error in streamQuery: {}", e.getMessage(), e);
			return Flux.error(e);
		}
	}

	@Override
	public boolean isHealthy() {
		return true;
	}

	@Override
	public String getName() {
		return "DemoAgent";
	}

	@Override
	public String getDescription() {
		return "A helpful assistant agent implemented in AgentScope.";
	}
}

