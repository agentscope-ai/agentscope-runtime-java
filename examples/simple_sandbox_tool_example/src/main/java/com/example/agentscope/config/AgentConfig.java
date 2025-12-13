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

package com.example.agentscope.config;

import com.example.agentscope.tools.CalculatorTool;
import com.example.agentscope.tools.WeatherTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * AgentScope configuration class
 * Configure Agent, Model, and Toolkit
 */
@Configuration
public class AgentConfig {

    @Value("${DASHSCOPE_API_KEY:#{null}}")
    private String apiKey;

    /**
     * Configure the DashScope chat model
     */
    @Bean
    public DashScopeChatModel chatModel() {
        String key = apiKey != null ? apiKey : System.getenv("DASHSCOPE_API_KEY");
        
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException(
                "DASHSCOPE_API_KEY is not configured. Please set env var: export DASHSCOPE_API_KEY=your-api-key"
            );
        }

        return DashScopeChatModel.builder()
                .apiKey(key)
                .modelName("qwen3-max")
                .build();
    }

    /**
     * Reuse SandboxService as a singleton
     */
    @Bean
    public SandboxService sandboxService() {
        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();

        SandboxService service = new SandboxService(
                new SandboxManager(managerConfig)
        );
        service.start();
        return service;
    }

    /**
     * Configure the toolkit and register all tools
     */
    @Bean
    public Toolkit createToolkit(SandboxService sandboxService) {
        Toolkit toolkit = new Toolkit();
        WeatherTool weatherTool = new WeatherTool();
        CalculatorTool calculatorTool = new CalculatorTool();

        toolkit.registerTool(weatherTool);
        toolkit.registerTool(calculatorTool);
        try {
            Sandbox sandbox = sandboxService.connect("userId", "sessionId", BrowserSandbox.class);
            toolkit.registerTool(ToolkitInit.BrowserNavigateTool(sandbox));
            if (sandbox instanceof BrowserSandbox browserSandbox) {
                String desktopUrl = browserSandbox.getDesktopUrl();
                System.out.println("GUI Desktop URL: " + desktopUrl);
            }
        } catch (Exception ignored) {
        }
        return toolkit;
    }

    /**
     * Create an independent Agent for each request to avoid shared state
     */
    @Bean
    @Scope("prototype")
    public ReActAgent createAgentInstance(DashScopeChatModel chatModel, Toolkit toolkit) {
        return ReActAgent.builder()
                .name("Smart Assistant")
                .sysPrompt("""
                        You are an intelligent assistant named "XiaoZhi". You can help users:
                        1. Query weather information
                        2. Perform mathematical calculations

                        Use the available tools to answer user questions and provide accurate and helpful responses.
                        Be friendly, professional, and clear in your answers.
                        """)
                .model(chatModel)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }
}

