package io.agentscope.chatbot;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.autoconfig.deployer.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Agent Server Application - Deploys the LLM agent service
 * Equivalent to agent_server.py
 */
@SpringBootApplication
public class AgentServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(AgentServerApplication.class);

    private static LocalDeployManager deployManager;
    private static ContextManager contextManager;
    private static SessionHistoryService sessionHistoryService;
    private static MemoryService memoryService;

    @Bean
    public Runner agentRunner() throws Exception {
        logger.info("Initializing Agent Runner...");

        // Get configuration from environment
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable not set");
        }

        // Initialize services
        sessionHistoryService = new InMemorySessionHistoryService();
        memoryService = new InMemoryMemoryService();

        sessionHistoryService.start().get();
        memoryService.start().get();

        // Create context manager
        contextManager = new ContextManager(
            ContextComposer.class,
            sessionHistoryService,
            memoryService
        );
        contextManager.start().get();

        // Create chat model
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // Create LLM Agent
        Builder builder = ReactAgent.builder()
                .name("llm_agent")
                .model(chatModel)
                .systemPrompt("You are a helpful AI assistant.");

        ReactAgent reactAgent = builder.build();

        // Wrap in SaaAgent
        SaaAgent saaAgent = SaaAgent.builder()
                .agentBuilder(reactAgent)
                .build();

        // Create runner
        Runner runner = Runner.builder()
                .agent(saaAgent)
                .contextManager(contextManager)
                .build();

        logger.info("✅ Agent Runner initialized successfully");

        return runner;
    }

    @PostConstruct
    public void deployAgent() throws Exception {
        logger.info("Deploying agent service...");

        // Get port from environment or use default
        String serverEndpoint = System.getenv("SERVER_ENDPOINT");
        if (serverEndpoint == null || serverEndpoint.isEmpty()) {
            serverEndpoint = "agent";
        }

        // Deploy using LocalDeployManager
        deployManager = new LocalDeployManager();
        deployManager.deployStreaming();

        logger.info("✅ Service deployed successfully!");
        logger.info("   Endpoint: http://localhost:8090/{}", serverEndpoint);
        logger.info("Agent Service is running...");
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutdown signal received. Stopping the service...");

        try {
            if (memoryService != null) {
                memoryService.stop().get();
            }
            if (sessionHistoryService != null) {
                sessionHistoryService.stop().get();
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }

        logger.info("✅ Service stopped.");
    }

    public static void main(String[] args) {
        SpringApplication.run(AgentServerApplication.class, args);
    }
}

