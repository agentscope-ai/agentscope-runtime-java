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

package io.agentscope;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
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


/**
 * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
 */
public class AgentScopeDeployExample {

    private DashScopeChatModel chatModel;
    private ContextManager contextManager;

    public AgentScopeDeployExample() {
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

            Toolkit toolkit = new Toolkit();
            toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
            toolkit.registerTool(ToolkitInit.RunShellCommandTool());
            toolkit.registerTool(ToolkitInit.BrowserNavigateTool());

            ReActAgent.Builder agent =
                    ReActAgent.builder()
                            .name("WebAgent")
                            .sysPrompt(
                                    "You are a helpful AI assistant. Provide clear and concise"
                                            + " answers.")
                            .toolkit(toolkit)
                            .memory(new InMemoryMemory())
                            .model(
                                    io.agentscope.core.model.DashScopeChatModel.builder()
                                            .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                                            .modelName("qwen-plus")
                                            .stream(true) // Enable streaming
                                            .enableThinking(true)
                                            .formatter(new DashScopeChatFormatter())
                                            .build());

            AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder().agent(agent).build();

            SandboxManager sandboxManager = new SandboxManager(ManagerConfig.builder().build());
            EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

            Runner runner = Runner.builder()
                    .agent(agentScopeAgent)
                    .contextManager(contextManager)
                    .environmentManager(environmentManager)
                    .build();

            LocalDeployManager.builder().port(10001).build().deploy(runner);

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

        AgentScopeDeployExample example = new AgentScopeDeployExample();

        try {
            example.basicExample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

