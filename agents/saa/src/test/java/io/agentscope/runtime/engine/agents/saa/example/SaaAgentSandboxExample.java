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

package io.agentscope.runtime.engine.agents.saa.example;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.engine.agents.saa.tools.ToolcallsInit;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import reactor.core.publisher.Flux;

/**
 * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
 */
public class SaaAgentSandboxExample {

    private EnvironmentManager environmentManager;
    private DashScopeChatModel chatModel;
    private ContextManager contextManager;

    public SaaAgentSandboxExample() {
        // Initialize DashScope ChatModel
        initializeChatModel();


        environmentManager = new DefaultEnvironmentManager();
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

    public CompletableFuture<Void> basicExample() {
        // Create Runner with the SaaAgent
        System.out.println("=== BaseSandboxTool Using SaaAgent Example ===");

        return CompletableFuture.supplyAsync(() -> {
            try {

                // Create ReactAgent Builder
                Builder builder = ReactAgent.builder()
                        .name("saa_agent")
                        .tools(List.of(ToolcallsInit.RunPythonCodeTool()))
                        .model(chatModel);

                // Create Runner with the SaaAgent
                SaaAgent saaAgent = SaaAgent.builder()
                        .agent(builder)
                        .build();

                Runner runner = Runner.builder()
                        .agent(saaAgent)
                        .contextManager(contextManager)
                        .environmentManager(environmentManager)
                        .build();

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
                throw new RuntimeException(e);
            }
        }).thenCompose(future -> future)
          .orTimeout(30, TimeUnit.SECONDS)
          .exceptionally(throwable -> {
              System.err.println("Operation failed or timed out: " + throwable.getMessage());
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

    /**
     * Main method to run all examples
     */
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
