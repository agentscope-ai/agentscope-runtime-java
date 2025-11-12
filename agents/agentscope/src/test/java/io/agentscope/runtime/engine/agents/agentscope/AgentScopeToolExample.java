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
import io.agentscope.core.model.DashScopeChatModel;
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
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AgentScopeToolExample {

    private ContextManager contextManager;

    public AgentScopeToolExample() {
        initializeContextManager();
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
        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();

        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.RunPythonCodeTool());

        return CompletableFuture.supplyAsync(() -> {
                    try {
                        ReActAgent.Builder agent =
                                ReActAgent.builder()
                                        .name("WebAgent")
                                        .sysPrompt(
                                                "You are a helpful AI assistant. Provide clear and concise"
                                                        + " answers.")
                                        .toolkit(toolkit)
                                        .memory(new InMemoryMemory())
                                        .model(
                                                DashScopeChatModel.builder()
                                                        .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                                                        .modelName("qwen-plus")
                                                        .stream(true) // Enable streaming
                                                        .enableThinking(false)
                                                        .formatter(new DashScopeChatFormatter())
                                                        .build());

                        AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder().agent(agent).build();

                        AgentRequest request = createAgentRequest("What is the 8th number of Fibonacci?", null, null);

                        Runner runner = Runner.builder().environmentManager(environmentManager).agent(agentScopeAgent).build();

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
                        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(e);
                        return failedFuture;
                    }
                }).thenCompose(future -> future)
                .orTimeout(30, TimeUnit.MINUTES)
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
        message.setType(MessageType.USER);
        message.setContent(List.of(textContent));

        List<Message> inputMessages = new ArrayList<>();
        inputMessages.add(message);
        request.setInput(inputMessages);

        return request;
    }

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


