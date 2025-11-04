//package runtime.domain.tools.service.example;
//
//import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
//import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
//import com.alibaba.cloud.ai.graph.agent.ReactAgent;
//import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
//import io.agentscope.runtime.engine.Runner;
//import io.agentscope.runtime.engine.agents.saa.SaaAgent;
//import io.agentscope.runtime.engine.memory.context.ContextComposer;
//import io.agentscope.runtime.engine.memory.context.ContextManager;
//import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
//import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
//import io.agentscope.runtime.engine.memory.service.MemoryService;
//import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
//import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
//import io.agentscope.runtime.engine.schemas.agent.Event;
//import io.agentscope.runtime.engine.schemas.agent.Message;
//import io.agentscope.runtime.engine.schemas.agent.TextContent;
//import reactor.core.publisher.Flux;
//
///**
// * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
// */
//public class SaaAgentExample {
//
//    private DashScopeChatModel chatModel;
//    private ContextManager contextManager;
//
//    public SaaAgentExample() {
//        // Initialize DashScope ChatModel
//        initializeChatModel();
//
//        // Initialize ContextManager (you may need to adapt this based on your actual implementation)
//        initializeContextManager();
//    }
//
//    private void initializeChatModel() {
//        // Create DashScopeApi instance using the API key from environment variable
//        DashScopeApi dashScopeApi = DashScopeApi.builder()
//                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
//                .build();
//
//        // Create DashScope ChatModel instance
//        this.chatModel = DashScopeChatModel.builder()
//                .dashScopeApi(dashScopeApi)
//                .build();
//    }
//
//    private void initializeContextManager() {
//        try {
//            // Create SessionHistoryService for managing conversation history
//            SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
//
//            // Create MemoryService for managing agent memory
//            MemoryService memoryService = new InMemoryMemoryService();
//
//            // Create ContextManager with the required services
//            this.contextManager = new ContextManager(
//                    ContextComposer.class,
//                    sessionHistoryService,
//                    memoryService
//            );
//
//            // Start the context manager services
//            sessionHistoryService.start().get();
//            memoryService.start().get();
//            this.contextManager.start().get();
//
//            System.out.println("ContextManager and its services initialized successfully");
//        } catch (Exception e) {
//            System.err.println("Failed to initialize ContextManager services: " + e.getMessage());
//            throw new RuntimeException("ContextManager initialization failed", e);
//        }
//    }
//
//    /**
//     * Basic example of using SaaAgent with ReactAgent
//     */
//    public CompletableFuture<Void> basicExample() {
//        System.out.println("=== Basic SaaAgent Example ===");
//        // Create Runner with the SaaAgent
//        Runner runner = new Runner(contextManager);
//
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                // Create ReactAgent Builder
//                ReactAgent reactAgent = ReactAgent.builder()
//                        .name("saa Agent")
//                        .description("saa Agent")
//                        .model(chatModel)
//                        .build();
//
//                ReactAgent proseAgent = ReactAgent.builder()
//                        .name("prose_agent")
//                        .description("prose writing expert")
//                        .instruction("You are a prose writing expert, skilled at writing prose.")
//                        .model(chatModel)
//                        .build();
//
//                ReactAgent poemAgent = ReactAgent.builder()
//                        .name("poem_agent")
//                        .description("poem writing expert")
//                        .instruction("You are a poetry writing expert, skilled at writing poetry.")
//                        .model(chatModel)
//                        .build();
//
//                LlmRoutingAgent llmRoutingAgent = LlmRoutingAgent.builder()
//                        .name("llm_routing_agent")
//                        .model(chatModel)
//                        .subAgents(List.of(proseAgent, poemAgent))
//                        .build();
//
//                // Create SaaAgent using the ReactAgent Builder
//                SaaAgent saaAgent = SaaAgent.builder()
//                        .agent(llmRoutingAgent)
//                        .build();
//
//                runner.registerAgent(saaAgent);
//
//                // Create AgentRequest
//                AgentRequest request = createAgentRequest("给我讲个笑话", null, null);
//
//                // Execute the agent and handle the response stream
//                Flux<Event> eventStream = runner.streamQuery(request);
//
//                // Create a CompletableFuture to handle the completion of the event stream
//                CompletableFuture<Void> completionFuture = new CompletableFuture<>();
//
//                eventStream.subscribe(
//                        this::handleEvent,
//                        error -> {
//                            System.err.println("Error occurred: " + error.getMessage());
//                            completionFuture.completeExceptionally(error);
//                        },
//                        () -> {
//                            System.out.println("Conversation completed.");
//                            completionFuture.complete(null);
//                        }
//                );
//
//                return completionFuture;
//            } catch (Exception e) {
//                e.printStackTrace();
//                CompletableFuture<Void> failedFuture = new CompletableFuture<>();
//                failedFuture.completeExceptionally(e);
//                return failedFuture;
//            }
//        }).thenCompose(future -> future)
//          .orTimeout(30, TimeUnit.MINUTES)  // Increase timeout to 5 minutes for LLM routing
//          .exceptionally(throwable -> {
//              System.err.println("Operation failed or timed out: " + throwable.getMessage());
//              throwable.printStackTrace();
//              return null;
//          });
//    }
//
//    /**
//     * Helper method to create AgentRequest
//     */
//    private AgentRequest createAgentRequest(String text, String userId, String sessionId) {
//        if (userId == null || userId.isEmpty()) {
//            userId = "default_user";
//        }
//        if (sessionId == null || sessionId.isEmpty()) {
//            sessionId = UUID.randomUUID().toString();
//        }
//        AgentRequest request = new AgentRequest();
//
//        request.setSessionId(sessionId);
//        request.setUserId(userId);
//
//        // Create text content
//        TextContent textContent = new TextContent();
//        textContent.setText(text);
//
//        // Create message
//        Message message = new Message();
//        message.setRole("user");
//        message.setContent(List.of(textContent));
//
//        // Set input messages
//        List<Message> inputMessages = new ArrayList<>();
//        inputMessages.add(message);
//        request.setInput(inputMessages);
//
//        return request;
//    }
//
//    /**
//     * Helper method to handle events from the agent
//     */
//    private void handleEvent(Event event) {
//        if (event instanceof Message message) {
//            System.out.println("Event - Type: " + message.getType() +
//                    ", Role: " + message.getRole() +
//                    ", Status: " + message.getStatus());
//
//            if (message.getContent() != null && !message.getContent().isEmpty()) {
//                TextContent content = (TextContent) message.getContent().get(0);
//                System.out.println("Content: " + content.getText());
//            }
//        } else {
//            System.out.println("Received event: " + event.getClass().getSimpleName());
//        }
//    }
//
//    /**
//     * Main method to run all examples
//     */
//    public static void main(String[] args) {
//        // Check if API key is set
//        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
//            System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
//            System.exit(1);
//        }
//
//        SaaAgentExample example = new SaaAgentExample();
//
//        try {
//            example.basicExample()
//                    .thenRun(() -> System.out.println("\n=== All examples completed ==="))
//                    .exceptionally(throwable -> {
//                        System.err.println("Example execution failed: " + throwable.getMessage());
//                        return null;
//                    })
//                    .join();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
