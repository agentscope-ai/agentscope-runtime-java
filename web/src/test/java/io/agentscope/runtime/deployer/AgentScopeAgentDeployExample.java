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

package io.agentscope.runtime.deployer;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;

import io.agentscope.runtime.adapters.agentscope.TestAgentScopeAgentAdapter;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

/**
 * Example demonstrating how to use SaaAgent to proxy ReactAgent and Runner to execute SaaAgent
 */
public class AgentScopeAgentDeployExample {

    private DashScopeChatModel chatModel;

    public AgentScopeAgentDeployExample() {
        // Initialize DashScope ChatModel
        initializeChatModel();

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

    /**
     * Basic example of using SaaAgent with ReactAgent
     */
    public void basicExample() {
        try {
            TestAgentScopeAgentAdapter agentAdapter = new TestAgentScopeAgentAdapter();

            agentAdapter.setStateService(new InMemoryStateService());
            agentAdapter.setSessionHistoryService(new InMemorySessionHistoryService());
            agentAdapter.setMemoryService(new InMemoryMemoryService());

//            SandboxService sandboxService = new SandboxService(
//                    new SandboxManager(ManagerConfig.builder().build())
//            );
//            agentAdapter.setSandboxService(sandboxService);

            AgentApp agentApp = new AgentApp(agentAdapter);
            agentApp.run();

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

        AgentScopeAgentDeployExample example = new AgentScopeAgentDeployExample();

        try {
            example.basicExample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
