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

import io.agentscope.runtime.adapters.agentscope.MyAgentScopeAgentHandler;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Example demonstrating how to use runtime to delegate AgentScope ReActAgent.
 */
public class AgentScopeAgentDeployExample {

    public AgentScopeAgentDeployExample() { }

    /**
     * Basic example of using AgentScope with ReActAgent
     */
    public static void basicExample() {
        try {
            MyAgentScopeAgentHandler agentAdapter = new MyAgentScopeAgentHandler();

            agentAdapter.setStateService(new InMemoryStateService());
            agentAdapter.setSessionHistoryService(new InMemorySessionHistoryService());
            agentAdapter.setMemoryService(new InMemoryMemoryService());

            agentAdapter.setSandboxService(buidSandboxService());

            AgentApp agentApp = new AgentApp(agentAdapter);
            agentApp.run();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private static SandboxService buidSandboxService() {
        BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        return sandboxService;
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

        try {
            basicExample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
