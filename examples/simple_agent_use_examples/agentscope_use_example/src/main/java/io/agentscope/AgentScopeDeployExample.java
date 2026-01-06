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

import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import org.jetbrains.annotations.NotNull;

/**
 * Example demonstrating how to use AgentScope to proxy ReActAgent
 */
public class AgentScopeDeployExample {
	public static void main(String[] args) {
		// Check if API key is set
		if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
			System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
			System.exit(1);
		}

		runAgent();
	}

	private static void runAgent() {
		MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
		agentHandler.setStateService(new InMemoryStateService());
		agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
		agentHandler.setMemoryService(new InMemoryMemoryService());
		agentHandler.setSandboxService(buidSandboxService());

		AgentApp agentApp = new AgentApp(agentHandler);
        agentApp.cors(registry -> registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true));
		agentApp.run(10001);
	}

	@NotNull
	private static SandboxService buidSandboxService() {
		BaseClientStarter clientConfig = DockerClientStarter.builder().build();
		ManagerConfig managerConfig = ManagerConfig.builder()
				.clientConfig(clientConfig)
				.build();
        SandboxService sandboxService = new SandboxService(
                managerConfig
        );
        sandboxService.start();
		return sandboxService;
	}
}

