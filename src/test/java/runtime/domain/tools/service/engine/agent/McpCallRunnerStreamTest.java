package runtime.domain.tools.service.engine.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
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
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.ToolsInit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class McpCallRunnerStreamTest {
    private DashScopeChatModel chatModel;
    private ContextManager contextManager;
    private SandboxManager sandboxManager;

    @BeforeEach
    void setUp() {
        initializeChatModel();
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

            System.out.println("ContextManager and its services initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize ContextManager services: " + e.getMessage());
            throw new RuntimeException("ContextManager initialization failed", e);
        }
    }

    @Test
    public void testToolCallStreamRunner() {
        System.out.println("=== Start BaseSandboxTool Call Stream Runner Test ===");
        Runner runner = new Runner(contextManager);
        try {
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

            List<ToolCallback> mcpTools = ToolsInit.getMcpTools(
                    mcpServerConfig,
                    SandboxType.BASE,
                    Runner.getSandboxManager());

            ReactAgent reactAgent = ReactAgent.builder()
                    .name("saa Agent")
                    .description("saa Agent")
                    .tools(mcpTools)
                    .model(chatModel)
                    .build();

            SaaAgent saaAgent = SaaAgent.builder()
                    .agent(reactAgent)
                    .build();

            runner.registerAgent(saaAgent);

            AgentRequest request = createAgentRequest("Tell me the time in New York", null, null);

            Flux<Event> eventStream = runner.streamQuery(request);

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

            completionFuture
                    .orTimeout(30, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        System.err.println("Operation failed or timed out: " + throwable.getMessage());
                        return null;
                    })
                    .join();

            Runner.getSandboxManager().cleanupAllSandboxes();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
