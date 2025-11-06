package runtime.domain.tools.service.example;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
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
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SaaAgentSandboxExample {

    private EnvironmentManager environmentManager;
    private DashScopeChatModel chatModel;
    private ContextManager contextManager;

    public SaaAgentSandboxExample() {
        initializeChatModel();
        environmentManager = new DefaultEnvironmentManager();
        initializeContextManager();
    }

    private void initializeChatModel() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    private void initializeContextManager() {
        try {
            SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
            MemoryService memoryService = new InMemoryMemoryService();
            this.contextManager = new ContextManager(
                    ContextComposer.class,
                    sessionHistoryService,
                    memoryService
            );

            sessionHistoryService.start().get();
            memoryService.start().get();
            this.contextManager.start().get();

        } catch (Exception e) {
            throw new RuntimeException("ContextManager initialization failed", e);
        }
    }

    public CompletableFuture<Void> basicExample() {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        Builder builder = ReactAgent.builder()
                                .name("saa_agent")
                                .tools(List.of(ToolcallsInit.RunPythonCodeTool()))
                                .model(chatModel);

                        SaaAgent saaAgent = SaaAgent.builder()
                                .agent(builder)
                                .build();

                        Runner runner = Runner.builder().agent(saaAgent).contextManager(contextManager).environmentManager(environmentManager).build();

                        AgentRequest request = createAgentRequest("What is the 8th number of Fibonacci?", null, null);

                        Flux<Event> eventStream = runner.streamQuery(request);

                        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

                        eventStream.subscribe(
                                this::handleEvent,
                                error -> {
                                    completionFuture.completeExceptionally(error);
                                },
                                () -> {
                                    completionFuture.complete(null);
                                }
                        );

                        return completionFuture;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).thenCompose(future -> future)
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(throwable -> null);
    }

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

        TextContent textContent = new TextContent();
        textContent.setText(text);

        Message message = new Message();
        message.setRole("user");
        message.setContent(List.of(textContent));

        List<Message> inputMessages = new ArrayList<>();
        inputMessages.add(message);
        request.setInput(inputMessages);

        return request;
    }

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

    public static void main(String[] args) {
        // Check if API key is set
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
            System.exit(1);
        }

        SaaAgentSandboxExample example = new SaaAgentSandboxExample();

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
            example.environmentManager.getSandboxManager().cleanupAllSandboxes();
            // Clean up all sandbox containers before exiting
            System.out.println("\n=== Cleaning up sandbox containers ===");
            try {
                example.environmentManager.getSandboxManager().cleanupAllSandboxes();
                System.out.println("=== Sandbox cleanup completed ===");
            } catch (Exception innerExe) {
                System.err.println("Error during sandbox cleanup: " + innerExe.getMessage());
                innerExe.printStackTrace();
            }
        }
    }
}


