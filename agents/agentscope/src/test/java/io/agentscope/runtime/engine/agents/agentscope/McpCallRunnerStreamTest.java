/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.engine.agents.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.schemas.message.MessageType;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.message.Event;
import io.agentscope.runtime.engine.schemas.message.Message;
import io.agentscope.runtime.engine.schemas.message.TextContent;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class McpCallRunnerStreamTest {
    private ContextManager contextManager;
    private EnvironmentManager environmentManager;

    @BeforeEach
    void setUp() {
        initializeContextManager();
        environmentManager = new DefaultEnvironmentManager();
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

            List<AgentTool> mcpTools = ToolkitInit.getMcpTools(
                    mcpServerConfig,
                    SandboxType.BASE,
                    environmentManager.getSandboxManager());

            Toolkit toolkit = new Toolkit();

            for(AgentTool tool: mcpTools){
                toolkit.registerTool(tool);
            }

            ReActAgent.Builder agent =
                    ReActAgent.builder()
                            .name("WebAgent")
                            .sysPrompt(
                                    "You are a helpful AI assistant. Provide clear and concise"
                                            + " answers.")
                            .memory(new InMemoryMemory())
                            .toolkit(toolkit)
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
                    .environmentManager(environmentManager)
                    .build();

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

            environmentManager.getSandboxManager().cleanupAllSandboxes();

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
        message.setType(MessageType.USER);
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
