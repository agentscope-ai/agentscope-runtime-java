package io.agentscope.runtime.deployer;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;


/**
 * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
 */
public class AgentScopeAgentDeployExample {

    private DashScopeChatModel chatModel;
    private ContextManager contextManager;

    public AgentScopeAgentDeployExample() {
        // Initialize DashScope ChatModel
        initializeChatModel();

        // Initialize ContextManager (you may need to adapt this based on your actual implementation)
        initializeContextManager();
    }

    private void initializeChatModel() {
        // Create DashScopeApi instance using the API key from environment variable
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // Create DashScope ChatModel instance
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    private void initializeContextManager() {
        try {
            // Create SessionHistoryService for managing conversation history
            SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();

            // Create MemoryService for managing agent memory
            MemoryService memoryService = new InMemoryMemoryService();

            // Create ContextManager with the required services
            this.contextManager = new ContextManager(
                    ContextComposer.class,
                    sessionHistoryService,
                    memoryService
            );

            // Start the context manager services
            sessionHistoryService.start().get();
            memoryService.start().get();
            this.contextManager.start().get();

            System.out.println("ContextManager and its services initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize ContextManager services: " + e.getMessage());
            throw new RuntimeException("ContextManager initialization failed", e);
        }
    }

    /**
     * Basic example of using SaaAgent with ReactAgent
     */
    public void basicExample() {
        try {

            Toolkit toolkit = new Toolkit();
            toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
            toolkit.registerTool(ToolkitInit.BrowserNavigateTool());

            ReActAgent.Builder agent =
                    ReActAgent.builder()
                            .name("WebAgent")
                            .sysPrompt(
                                    "You are a helpful AI assistant. Provide clear and concise"
                                            + " answers.")
                            .toolkit(toolkit)
                            .memory(new InMemoryMemory())
                            .model(
                                    io.agentscope.core.model.DashScopeChatModel.builder()
                                            .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                                            .modelName("qwen-plus")
                                            .stream(true) // Enable streaming
                                            .enableThinking(true)
                                            .formatter(new DashScopeChatFormatter())
                                            .build());

            AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder().agent(agent).build();

            Runner runner = Runner.builder()
                    .agent(agentScopeAgent)
                    .contextManager(contextManager)
                    .environmentManager(new DefaultEnvironmentManager())
                    .build();
            new LocalDeployManager().deploy(runner);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        // Check if API key is set
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
            System.exit(1);
        }

        AgentScopeAgentDeployExample example = new AgentScopeAgentDeployExample();

        try {
            example.basicExample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
