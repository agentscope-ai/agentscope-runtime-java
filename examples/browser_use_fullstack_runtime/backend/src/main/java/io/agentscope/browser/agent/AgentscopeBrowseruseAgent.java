package io.agentscope.browser.agent;

import com.alibaba.cloud.ai.agent.Agent;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import io.agentscope.browser.constants.Prompts;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.agents.saa.tools.ToolcallsInit;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.session.RedisSessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AgentScope browser use agent implementation in Java
 */
public class AgentscopeBrowseruseAgent {

    private static final Logger logger = LoggerFactory.getLogger(AgentscopeBrowseruseAgent.class);

    private static final String USER_ID = "user_1";
    private static final String SESSION_ID = "session_001";  // Using a fixed ID for simplicity

    private List<ToolCallback> tools;
    private SaaAgent agent;
    private Runner runner;
    private ContextManager contextManager;
    private EnvironmentManager environmentManager;
    private MemoryService memoryService;
    private SessionHistoryService sessionHistoryService;
    private String browserWebSocketUrl;

    public AgentscopeBrowseruseAgent() {
        initializeTools();
    }

    private void initializeTools() {
        // Initialize all browser tools
        this.tools = new ArrayList<>();

        // Base tools
        tools.add(ToolcallsInit.RunShellCommandTool());
        tools.add(ToolcallsInit.RunPythonCodeTool());

        // Browser tools
        tools.add(ToolcallsInit.BrowserCloseTool());
        tools.add(ToolcallsInit.BrowserResizeTool());
        tools.add(ToolcallsInit.BrowserConsoleMessagesTool());
        tools.add(ToolcallsInit.BrowserHandleDialogTool());
        tools.add(ToolcallsInit.BrowserFileUploadTool());
        tools.add(ToolcallsInit.BrowserPressKeyTool());
        tools.add(ToolcallsInit.BrowserNavigateTool());
        tools.add(ToolcallsInit.BrowserNavigateBackTool());
        tools.add(ToolcallsInit.BrowserNavigateForwardTool());
        tools.add(ToolcallsInit.BrowserNetworkRequestsTool());
        tools.add(ToolcallsInit.BrowserPdfSaveTool());
        tools.add(ToolcallsInit.BrowserTakeScreenshotTool());
        tools.add(ToolcallsInit.BrowserSnapshotTool());
        tools.add(ToolcallsInit.BrowserClickTool());
        tools.add(ToolcallsInit.BrowserDragTool());
        tools.add(ToolcallsInit.BrowserHoverTool());
        tools.add(ToolcallsInit.BrowserTypeTool());
        tools.add(ToolcallsInit.BrowserSelectOptionTool());
        tools.add(ToolcallsInit.BrowserTabListTool());
        tools.add(ToolcallsInit.BrowserTabNewTool());
        tools.add(ToolcallsInit.BrowserTabSelectTool());
        tools.add(ToolcallsInit.BrowserTabCloseTool());
        tools.add(ToolcallsInit.BrowserWaitForTool());
    }

    /**
     * Connect and initialize the agent
     */
    public void connect() throws Exception {
        logger.info("Initializing AgentscopeBrowseruseAgent...");

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

        SandboxManager sandboxManager = new SandboxManager(ManagerConfig.builder().containerDeployment(KubernetesClientConfig.builder().build()).build());
//        SandboxManager sandboxManager = new SandboxManager(ManagerConfig.builder().containerDeployment(new DockerClientConfig(true, "127.0.0.1", 64352, null)).build());
        environmentManager = new DefaultEnvironmentManager(sandboxManager);

        // Initialize chat model
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable not set");
        }

        // Create SaaAgent
        agent = SaaAgent.builder()
                .agent(createOrignalAgentBuilder(apiKey))
                .build();

        // Initialize runner
        runner = Runner.builder()
                .agent(agent)
                .contextManager(contextManager)
                .environmentManager(environmentManager)
                .build();

        // Get browser WebSocket URL
        try {
            // Connect to browser sandbox
            ContainerModel sandboxInfo = environmentManager.getSandboxManager().getSandbox(SandboxType.BROWSER, USER_ID, SESSION_ID);

            if (sandboxInfo != null ) {
                browserWebSocketUrl = sandboxInfo.getFrontBrowserWS();
                logger.info("Browser WebSocket URL: {}", browserWebSocketUrl);
            } else {
                browserWebSocketUrl = "";
                logger.warn("No browser WebSocket URL found");
            }
        } catch (Exception e) {
            logger.warn("Failed to get browser WebSocket URL: {}", e.getMessage());
            browserWebSocketUrl = "";
        }

        logger.info("AgentscopeBrowseruseAgent initialized successfully");
    }

    private com.alibaba.cloud.ai.graph.agent.Builder createOrignalAgentBuilder(String apiKey) throws GraphStateException {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // Create ReactAgent
        return ReactAgent.builder()
                .name("Friday")
                .tools(tools)
                .model(chatModel)
                .systemPrompt(Prompts.SYSTEM_PROMPT);
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

        for (Map<String, String> chatMessage : chatMessages) {
            Message message = new Message();
            message.setRole(chatMessage.get("role"));

            TextContent textContent = new TextContent();
            textContent.setText(chatMessage.get("content"));
            message.setContent(List.of(textContent));

            convertedMessages.add(message);
        }

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
     * Close and cleanup resources
     */
    public void close() throws Exception {
        logger.info("Closing AgentscopeBrowseruseAgent...");

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

        logger.info("AgentscopeBrowseruseAgent closed");
    }
}

