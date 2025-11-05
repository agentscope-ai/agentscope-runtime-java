package io.agentscope.runtime.engine.agents.saa.example;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.agents.saa.tools.MCPToolExecutor;
import io.agentscope.runtime.engine.agents.saa.tools.McpConfigConverter;
import io.agentscope.runtime.engine.agents.saa.tools.ToolcallsInit;
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
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import reactor.core.publisher.Flux;


public class SaaAgentMCPSandboxExample {

    private DashScopeChatModel chatModel;
    private ContextManager contextManager;
    private EnvironmentManager environmentManager;

    public SaaAgentMCPSandboxExample() {
        initializeChatModel();
        initializeContextManager();
        this.environmentManager = new DefaultEnvironmentManager();
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
                    memoryService);

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
    public CompletableFuture<Void> basicExample() {
        System.out.println("=== SaaAgent example Using custom MCP server ===");

        String mcpServerConfig = """
                           {
                    "mcpServers": {
                        "time": {
                            "command": "uvx",
                            "args": [
                                "mcp-server-time",
                                "--local-timezone=America/New_York"
                            ]
                        }
                    }
                }
                """;

        try  {
            List<ToolCallback> mcpTools = ToolcallsInit.getMcpTools(
                    mcpServerConfig,
                    SandboxType.BASE,
                    environmentManager.getSandboxManager());

            System.out.println("Created " + mcpTools.size() + " MCP tools");
            mcpTools.forEach(tool -> System.out.println("  - " + tool));

            McpConfigConverter converter = McpConfigConverter.builder()
                    .serverConfigs(mcpServerConfig)
                    .sandboxType(SandboxType.BASE)
                    .sandboxManager(environmentManager.getSandboxManager())
                    .build();

            List<MCPTool> mcpToolInstances = converter.toBuiltinTools();
            System.out.println("Created " + mcpToolInstances.size() + " MCPTool instances");

            List<ToolCallback> toolCallbacks = mcpToolInstances.stream()
                    .map(mcpTool -> new MCPToolExecutor(mcpTool).buildTool())
                    .collect(Collectors.toUnmodifiableList());

            System.out.println("Built " + toolCallbacks.size() + " ToolCallbacks");

            return CompletableFuture.supplyAsync(() -> {
                        try {

                            // Create ReactAgent Builder
                            Builder builder = ReactAgent.builder()
                                    .name("saa_agent")
                                    .tools(toolCallbacks)
                                    .model(chatModel);

                            // Create SaaAgent using the ReactAgent Builder
                            SaaAgent saaAgent = SaaAgent.builder()
                                    .agent(builder)
                                    .build();

                            // Create AgentRequest
                            AgentRequest request = createAgentRequest("Get current time for me.", null, null);

                            Runner runner = new Runner(saaAgent, contextManager, environmentManager);

                            // Execute the agent and handle the response stream
                            Flux<Event> eventStream = runner.streamQuery(request);

                            // Create a CompletableFuture to handle the completion of the event stream
                            CompletableFuture<Void> completionFuture = new CompletableFuture<>();

                            eventStream.subscribe(
                                    this::handleEvent,
                                    error -> {
                                        System.err.println("Error occurred: " + error.getMessage());
                                        completionFuture.completeExceptionally(error);
                                    },
                                    () -> {
                                        System.out.println("Conversation completed.");
                                        completionFuture.complete(null);
                                    });

                            return completionFuture;
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }).thenCompose(future -> future)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        System.err.println("Operation failed or timed out: " + throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Error in basic MCP tool example: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper method to create AgentRequest
     */
    private AgentRequest createAgentRequest(String text, String userId, String sessionId) {
        if (userId == null || userId.isEmpty()) {
            userId = "default_user";
        }
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        AgentRequest request = new AgentRequest();

        request.setSessionId(sessionId);
        request.setUserId(userId);

        // Create text content
        TextContent textContent = new TextContent();
        textContent.setText(text);

        // Create message
        Message message = new Message();
        message.setRole("user");
        message.setContent(List.of(textContent));

        // Set input messages
        List<Message> inputMessages = new ArrayList<>();
        inputMessages.add(message);
        request.setInput(inputMessages);

        return request;
    }

    /**
     * Helper method to handle events from the agent
     */
    private void handleEvent(Event event) {
        if (event instanceof Message message) {
            System.out.println("Event - Type: " + message.getType() +
                    ", Role: " + message.getRole() +
                    ", Status: " + message.getStatus());

            if (message.getContent() != null && !message.getContent().isEmpty()) {
                TextContent content = (TextContent) message.getContent().get(0);
                System.out.println("Content: " + content.getText());
            }
        } else {
            System.out.println("Received event: " + event.getClass().getSimpleName());
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

        SaaAgentMCPSandboxExample example = new SaaAgentMCPSandboxExample();

        try {
            example.basicExample()
                    .thenRun(() -> System.out.println("\n=== All examples completed ==="))
                    .exceptionally(throwable -> {
                        System.err.println("Example execution failed: " + throwable.getMessage());
                        return null;
                    })
                    .join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up all sandbox containers before exiting
            System.out.println("\n=== Cleaning up sandbox containers ===");
            try {
                example.environmentManager.getSandboxManager().cleanupAllSandboxes();
                System.out.println("=== Sandbox cleanup completed ===");
            } catch (Exception e) {
                System.err.println("Error during sandbox cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
