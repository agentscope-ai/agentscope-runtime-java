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

import io.agentscope.browser.BrowserAgentApplication;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AgentScope browser use agent implementation in Java
 */
public class AgentscopeBrowserUseAgent {

    private static final Logger logger = LoggerFactory.getLogger(AgentscopeBrowserUseAgent.class);

    private static final String USER_ID = "user_1";
    private static final String SESSION_ID = "session_001";  // Using a fixed ID for simplicity

    private Runner runner;
    private ContextManager contextManager;
    private EnvironmentManager environmentManager;
    private MemoryService memoryService;
    private SessionHistoryService sessionHistoryService;
    private String browserWebSocketUrl;
    private String baseUrl;
    private String runtimeToken;
    private final BrowserAgentApplication.RunnerHolder runnerHolder;

    public AgentscopeBrowserUseAgent(BrowserAgentApplication.RunnerHolder runnerHolder) {
        this.runnerHolder = runnerHolder;
    }

    /**
     * Connect and initialize the agent
     */
    public void connect() throws Exception {
        logger.info("Initializing AgentscopeBrowserUseAgent...");

        // Initialize session history service
        sessionHistoryService = new InMemorySessionHistoryService();
        // Create session
        sessionHistoryService.createSession(USER_ID, Optional.of(SESSION_ID)).get();
        // Initialize memory service
        memoryService = new InMemoryMemoryService();

        // Initialize context manager
        contextManager = new ContextManager(
                ContextComposer.class,
                sessionHistoryService,
                memoryService
        );
        contextManager.start().get();

//        AgentRun, Kubernetes, Docker are supported to run sandboxes. When not configured, use Docker as default choice.
//        BaseClientConfig clientConfig = AgentRunClientConfig.builder()
//                .agentRunAccessKeyId(System.getenv("AGENT_RUN_ACCESS_KEY_ID"))
//                .agentRunAccountId(System.getenv("AGENT_RUN_ACCOUNT_ID"))
//                .agentRunAccessKeySecret(System.getenv("AGENT_RUN_ACCESS_KEY_SECRET"))
//                .build();

//        BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();

        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();

        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        this.environmentManager = new DefaultEnvironmentManager(sandboxManager);

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.BrowserNavigateTool());

        // Initialize chat model
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable not set");
        }

        // Create SaaAgent
        ReActAgent.Builder agentBuilder =
                ReActAgent.builder()
                        .name("Assistant")
                        .sysPrompt("You are a helpful AI assistant. Be friendly and concise.")
                        .model(
                                io.agentscope.core.model.DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName("qwen-plus")
                                        .stream(true)
                                        .enableThinking(true)
                                        .formatter(new DashScopeChatFormatter())
                                        .defaultOptions(
                                                GenerateOptions.builder()
                                                        .thinkingBudget(1024)
                                                        .build())
                                        .build())
                        .memory(new InMemoryMemory())
                        .toolkit(toolkit);

        AgentScopeAgent agent = AgentScopeAgent.builder()
                .agent(agentBuilder)
                .build();

        // Initialize runner
        runner = Runner.builder().agent(agent).contextManager(contextManager).environmentManager(environmentManager).build();
        // Set runner to the holder so it can be accessed as a bean
        runnerHolder.setRunner(runner);

        // Get browser WebSocket URL and VNC info
        try {
            // Connect to browser sandbox
            ContainerModel sandboxInfo = environmentManager.getSandboxManager().getSandbox(SandboxType.BROWSER, USER_ID, SESSION_ID);

            if (sandboxInfo != null ) {
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
    }

    public Flux<Message> chatSimple(String userMessage) {
        // Convert chat messages to agent request format
        List<Message> convertedMessages = new ArrayList<>();

        Message message = new Message();
        message.setRole("user");

        TextContent textContent = new TextContent();
        textContent.setText(userMessage);
        message.setContent(List.of(textContent));

        convertedMessages.add(message);

        // Create agent request
        AgentRequest request = new AgentRequest();
        request.setSessionId(SESSION_ID);
        request.setUserId(USER_ID);
        request.setInput(convertedMessages);

        // Stream query
        Flux<Event> eventStream = runner.streamQuery(request);

        // Transform events to message content
        return eventStream
                .filter(event -> event instanceof Message)
                .map(event -> (Message) event);
    }

    /**
     * Chat with the agent
     * @param chatMessages List of chat messages
     * @return Flux of response messages
     */
    public Flux<Message> chat(List<Map<String, String>> chatMessages) {
        // Convert chat messages to agent request format
        List<Message> convertedMessages = new ArrayList<>();

        Message message = new Message();
        message.setRole(chatMessages.get(chatMessages.size()-1).get("role"));

        TextContent textContent = new TextContent();
        textContent.setText(chatMessages.get(chatMessages.size()-1).get("content"));
        message.setContent(List.of(textContent));

        convertedMessages.add(message);

        System.out.println("Converted: ");
        System.out.println(convertedMessages);

        // Create agent request
        AgentRequest request = new AgentRequest();
        request.setSessionId(SESSION_ID);
        request.setUserId(USER_ID);
        request.setInput(convertedMessages);

        // Stream query
        Flux<Event> eventStream = runner.streamQuery(request);

        // Transform events to message content
        return eventStream
                .filter(event -> event instanceof Message)
                .map(event -> (Message) event);
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

        if (memoryService != null) {
            memoryService.stop().get();
        }

        if (sessionHistoryService != null) {
            sessionHistoryService.stop().get();
        }

        // Cleanup sandboxes
        try {
            environmentManager.getSandboxManager().cleanupAllSandboxes();
        } catch (Exception e) {
            logger.warn("Failed to cleanup sandboxes: {}", e.getMessage());
        }

        logger.info("AgentscopeBrowserUseAgent closed");
    }
}


