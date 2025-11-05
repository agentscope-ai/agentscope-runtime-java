package runtime.domain.tools.service.example;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.autoconfig.deployer.LocalDeployManager;
import io.agentscope.runtime.autoconfig.deployer.ServerConfig;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.agents.saa.tools.ToolcallsInit;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

import java.util.List;

/**
 * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
 */
public class SaaAgentRemoteDeployExample {

    private DashScopeChatModel chatModel;
    private ContextManager contextManager;

    public SaaAgentRemoteDeployExample() {
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
            // Create ReactAgent Builder
            Builder builder = ReactAgent.builder()
                    .name("saa_agent")
                    .model(chatModel)
                    .tools(List.of(ToolcallsInit.RunPythonCodeTool()));

            // Create SaaAgent using the ReactAgent Builder
            SaaAgent saaAgent = SaaAgent.builder()
                    .agent(builder)
                    .build();

            ManagerConfig managerConfig = ManagerConfig.builder()
                    .baseUrl("http://localhost:10001/")
                    .build();

            EnvironmentManager environmentManager = new DefaultEnvironmentManager(new SandboxManager(managerConfig));

            Runner runner = new Runner(saaAgent, contextManager, environmentManager);

            ServerConfig serverConfig = new ServerConfig(10002);

            LocalDeployManager deployManager = new LocalDeployManager();

            deployManager.deployStreaming("process", serverConfig);

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

        SaaAgentRemoteDeployExample example = new SaaAgentRemoteDeployExample();

        try {
            example.basicExample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
