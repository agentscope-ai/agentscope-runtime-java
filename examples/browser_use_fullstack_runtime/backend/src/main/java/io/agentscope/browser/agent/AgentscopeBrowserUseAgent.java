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

package io.agentscope.browser.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.AgentScopeAgentHandler;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static io.agentscope.browser.constants.Prompts.SYSTEM_PROMPT;

/**
 * AgentScope browser use agent implementation in Java
 * 
 * <p>This implementation follows the following logic:
 * <ul>
 *   <li>Exports state from StateService</li>
 *   <li>Creates ReActAgent with MemoryAdapter and LongTermMemoryAdapter</li>
 *   <li>Loads state if available</li>
 *   <li>Streams agent responses</li>
 *   <li>Saves state after completion</li>
 * </ul>
 */
public class AgentscopeBrowserUseAgent extends AgentScopeAgentHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentscopeBrowserUseAgent.class);

    private final String apiKey;
    private String browserWebSocketUrl;
    private String baseUrl;
    private String runtimeToken;

    private volatile boolean connected = false;

    /**
     * Creates a new AgentscopeBrowserUseAgent.
     */
    public AgentscopeBrowserUseAgent() {
        // Get API key from environment
        String dashscopeKey = System.getenv("DASHSCOPE_API_KEY");
        if (dashscopeKey == null || dashscopeKey.isEmpty()) {
            dashscopeKey = System.getenv("AI_DASHSCOPE_API_KEY");
        }
        if (dashscopeKey == null || dashscopeKey.isEmpty()) {
            throw new RuntimeException("DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable not set");
        }
        this.apiKey = dashscopeKey;
    }

    /**
     * Connect and initialize the agent
     */
    public synchronized Sandbox connect(String sessionId, String userId) {
        logger.info("Initializing sand box...");
        Sandbox sandbox = null;
        // Get browser WebSocket URL and VNC info
        try {
            sandbox = this.sandboxService.connect(sessionId, userId, BrowserSandbox.class);

            // Connect to browser sandbox - use default user/session IDs for initialization
            ContainerModel sandboxInfo = sandbox.getInfo();

            if (sandboxInfo != null) {
                browserWebSocketUrl = sandboxInfo.getFrontBrowserWS();
                baseUrl = sandboxInfo.getBaseUrl();
                runtimeToken = sandboxInfo.getRuntimeToken();
                logger.info("Browser WebSocket URL: {}", browserWebSocketUrl);
                logger.info("Base URL: {}", baseUrl);
                logger.info("Runtime Token: {}", runtimeToken);
            } else {
                browserWebSocketUrl = "";
                baseUrl = "";
                runtimeToken = "";
                logger.warn("No browser sandbox info found");
            }
        } catch (Exception e) {
            logger.warn("Failed to get browser sandbox info: {}", e.getMessage());
            browserWebSocketUrl = "";
            baseUrl = "";
            runtimeToken = "";
        }

        logger.info("AgentscopeBrowserUseAgent initialized successfully");
        return sandbox;
    }

    @Override
    public Flux<Event> streamQuery(AgentRequest request, Object messages) {
        String sessionId = request.getSessionId();
        String userId = request.getUserId();

        try {
            // Step 1: Export state from StateService
            Map<String, Object> state = null;
            if (stateService != null) {
                try {
                    state = stateService.exportState(userId, sessionId, null).join();
                } catch (Exception e) {
                    logger.warn("Failed to export state: {}", e.getMessage());
                }
            }

            // Step 2: Create Toolkit and register tools
            Toolkit toolkit = new Toolkit();

            if (sandboxService != null) {
                try {
                    Sandbox sandbox = connect(sessionId, userId);

                    // Register Python code execution tool (matching Python: execute_python_code)
                    toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
                    // Register browser navigation tool
                    toolkit.registerTool(ToolkitInit.BrowserNavigateTool(sandbox));

                    logger.debug("Registered execute_python_code tool");
                } catch (Exception e) {
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
                    .name("Assistant")
                    .sysPrompt(SYSTEM_PROMPT)
                    .toolkit(toolkit)
                    .model(
                            DashScopeChatModel.builder()
                                    .apiKey(apiKey)
                                    .modelName("qwen-max")
                                    .stream(true)
                                    .enableThinking(true)
                                    .formatter(new DashScopeChatFormatter())
                                    .defaultOptions(
                                            GenerateOptions.builder()
                                                    .thinkingBudget(1024)
                                                    .build())
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
                } catch (Exception e) {
                    logger.warn("Failed to load state: {}", e.getMessage());
                }
            }

            // Step 7: Convert messages parameter to List<Msg>
            List<Msg> agentMessages;
            if (messages instanceof List) {
                @SuppressWarnings("unchecked")
                List<Msg> msgList = (List<Msg>) messages;
                agentMessages = msgList;
            } else if (messages instanceof Msg) {
                agentMessages = List.of((Msg) messages);
            } else {
                logger.warn("Unexpected messages type: {}, using empty list",
                        messages != null ? messages.getClass().getName() : "null");
                agentMessages = List.of();
            }

            // Step 8: Stream agent responses
            StreamOptions streamOptions = StreamOptions.builder()
                    .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                    .incremental(true)
                    .build();

            Msg queryMessage;
            if (agentMessages.isEmpty()) {
                queryMessage = Msg.builder()
                        .role(io.agentscope.core.message.MsgRole.USER)
                        .build();
            } else if (agentMessages.size() == 1) {
                queryMessage = agentMessages.get(0);
            } else {
                // Multiple messages - add all but the last to memory, then use the last one for query
                for (int i = 0; i < agentMessages.size() - 1; i++) {
                    agent.getMemory().addMessage(agentMessages.get(i));
                }
                queryMessage = agentMessages.get(agentMessages.size() - 1);
            }

            // Stream agent responses
            Flux<Event> agentScopeEvents = agent.stream(queryMessage, streamOptions);

            // Return raw framework stream - Runner will handle conversion via StreamAdapter
            return agentScopeEvents
                    .doOnNext(event -> {
                        logger.info("Agent event: {}", event);
                    })
                    .doFinally(signalType -> {
                        // Step 9: Save state after completion
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
                            } catch (Exception e) {
                                logger.error("Error saving state: {}", e.getMessage(), e);
                            }
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Error in agent stream: {}", error.getMessage(), error);
                    });

        } catch (Exception e) {
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
        return "BrowserAgent";
    }

    @Override
    public String getDescription() {
        return "A browser-enabled AI assistant agent implemented in AgentScope.";
    }

    /**
     * Get browser WebSocket URL
     */
    public String getBrowserWebSocketUrl() {
        return browserWebSocketUrl;
    }

    /**
     * Get base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get runtime token
     */
    public String getRuntimeToken() {
        return runtimeToken;
    }

    /**
     * Close and cleanup resources
     */
    public void close() throws Exception {
        logger.info("Closing AgentscopeBrowserUseAgent...");


        logger.info("AgentscopeBrowserUseAgent closed");
    }
}


