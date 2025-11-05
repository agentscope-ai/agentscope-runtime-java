package runtime.domain.tools.service.example;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
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
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
 */
public class AgentScopeToolExample {

    private ContextManager contextManager;

    public AgentScopeToolExample() {
        // Initialize ContextManager (you may need to adapt this based on your actual implementation)
        initializeContextManager();
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
    public CompletableFuture<Void> basicExample() {
        System.out.println("=== Basic SaaAgent Example ===");

//        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        BaseClientConfig clientConfig = KubernetesClientConfig.builder().kubeConfigPath("/Users/xht/Downloads/agentscope-runtime-java/kubeconfig.txt").build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();

        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

        Runner runner = new Runner(environmentManager);

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.RunPythonCodeTool());

        return CompletableFuture.supplyAsync(() -> {
                    try {
                        ReActAgent.Builder agentBuilder =
                                ReActAgent.builder()
                                        .name("Assistant")
                                        .sysPrompt("You are a helpful AI assistant. Be friendly and concise.")
                                        .model(
                                                DashScopeChatModel.builder()
                                                        .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
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

                        AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder().agent(agentBuilder).build();

                        runner.registerAgent(agentScopeAgent);

                        // Create AgentRequest
                        AgentRequest request = createAgentRequest("What is the 8th number of Fibonacci?", null, null);

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
                                }
                        );

                        return completionFuture;
                    } catch (Exception e) {
                        e.printStackTrace();
                        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(e);
                        return failedFuture;
                    }
                }).thenCompose(future -> future)
                .orTimeout(30, TimeUnit.MINUTES)  // Increase timeout to 5 minutes for LLM routing
                .exceptionally(throwable -> {
                    System.err.println("Operation failed or timed out: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return null;
                });
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

        AgentScopeToolExample example = new AgentScopeToolExample();

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
        }
    }
}
