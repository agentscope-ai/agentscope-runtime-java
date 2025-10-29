package io.agentscope.browser.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.browser.constants.Prompts;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.DataContent;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.ToolsInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
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
        tools.add(ToolsInit.RunShellCommandTool());
        tools.add(ToolsInit.RunPythonCodeTool());

        // Browser tools
        tools.add(ToolsInit.BrowserCloseTool());
        tools.add(ToolsInit.BrowserResizeTool());
        tools.add(ToolsInit.BrowserConsoleMessagesTool());
        tools.add(ToolsInit.BrowserHandleDialogTool());
        tools.add(ToolsInit.BrowserFileUploadTool());
        tools.add(ToolsInit.BrowserPressKeyTool());
        tools.add(ToolsInit.BrowserNavigateTool());
        tools.add(ToolsInit.BrowserNavigateBackTool());
        tools.add(ToolsInit.BrowserNavigateForwardTool());
        tools.add(ToolsInit.BrowserNetworkRequestsTool());
        tools.add(ToolsInit.BrowserPdfSaveTool());
        tools.add(ToolsInit.BrowserTakeScreenshotTool());
        tools.add(ToolsInit.BrowserSnapshotTool());
        tools.add(ToolsInit.BrowserClickTool());
        tools.add(ToolsInit.BrowserDragTool());
        tools.add(ToolsInit.BrowserHoverTool());
        tools.add(ToolsInit.BrowserTypeTool());
        tools.add(ToolsInit.BrowserSelectOptionTool());
        tools.add(ToolsInit.BrowserTabListTool());
        tools.add(ToolsInit.BrowserTabNewTool());
        tools.add(ToolsInit.BrowserTabSelectTool());
        tools.add(ToolsInit.BrowserTabCloseTool());
        tools.add(ToolsInit.BrowserWaitForTool());
    }

    /**
     * Connect and initialize the agent
     */
    public void connect() throws Exception {
        logger.info("Initializing AgentscopeBrowseruseAgent...");

        // Initialize session history service
        sessionHistoryService = new InMemorySessionHistoryService();
        sessionHistoryService.start().get();

        // Create session
        sessionHistoryService.createSession(USER_ID, Optional.of(SESSION_ID)).get();

        // Initialize memory service
        memoryService = new InMemoryMemoryService();
        memoryService.start().get();

        // Initialize context manager
        contextManager = new ContextManager(
            ContextComposer.class,
            sessionHistoryService,
            memoryService
        );
        contextManager.start().get();

        // Initialize chat model
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable not set");
        }

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // Create ReactAgent
        Builder builder = ReactAgent.builder()
                .name("Friday")
                .tools(tools)
                .model(chatModel)
                .systemPrompt(Prompts.SYSTEM_PROMPT);

        ReactAgent reactAgent = builder.build();

        // Create SaaAgent
        agent = SaaAgent.builder()
                .agentBuilder(reactAgent)
                .build();

        // Initialize runner
        runner = new Runner(contextManager);
        runner.registerAgent(agent);

        // Get browser WebSocket URL
        try {
            // Connect to browser sandbox
            Map<String, Object> sandboxInfo = Runner.getSandboxManager()
                    .getSandboxInfo(SESSION_ID, USER_ID, SandboxType.BROWSER);

            if (sandboxInfo != null && sandboxInfo.containsKey("front_browser_ws")) {
                browserWebSocketUrl = (String) sandboxInfo.get("front_browser_ws");
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

    /**
     * Chat with the agent
     * @param chatMessages List of chat messages
     * @return Flux of response messages
     */
    public Flux<List<Object>> chat(List<Map<String, String>> chatMessages) {
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
                .map(event -> {
                    Message message = (Message) event;
                    if (message.getContent() != null && !message.getContent().isEmpty()) {
                        return message.getContent();
                    }
                    return List.of();
                })
                .filter(content -> !content.isEmpty());
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
            Runner.getSandboxManager().cleanupAllSandboxes();
        } catch (Exception e) {
            logger.warn("Failed to cleanup sandboxes: {}", e.getMessage());
        }

        logger.info("AgentscopeBrowseruseAgent closed");
    }
}

