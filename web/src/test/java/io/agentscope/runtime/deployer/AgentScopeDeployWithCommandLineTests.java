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

import java.net.URL;
import java.util.Objects;
import java.util.Properties;

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.adapters.agentscope.MyAgentScopeAgentHandler;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import org.junit.jupiter.api.Test;

public class AgentScopeDeployWithCommandLineTests {

	@Test
	void testRunWithCommandLine(){
		URL resource = Thread.currentThread().getContextClassLoader().getResource(".env");
		AgentApp app;
		if (Objects.nonNull(resource)){
			String[] commandLine = new String[2];
			commandLine[0] = "-f";
			commandLine[1] = Objects.requireNonNull(resource).getPath();
			app = new AgentApp(commandLine);
		}else{
			// ci can not get the .env file, here it is injected through the system properties
			System.setProperty("agent.app.port","7779");
			System.setProperty("agent.app.handler.provider.class","io.agentscope.runtime.deployer.AgentScopeDeployWithCommandLineTests$MyAgentHandlerProvider");
			System.setProperty("agent.app.sandbox.service.provider.class","io.agentscope.runtime.deployer.AgentScopeDeployWithCommandLineTests$MySandboxServiceProvider");
			System.setProperty("AI_DASHSCOPE_API_KEY","your key");
			app = new AgentApp(new String[0]);
		}
		app.run();
	}

	public static class MyAgentHandlerProvider implements AgentApp.AgentHandlerProvider{

		@Override
		public AgentHandler get(Properties properties, AgentApp.ServiceComponentManager serviceComponentManager) {
			MyAgentScopeAgentHandler handler = new MyAgentScopeAgentHandler();
			handler.setStateService(serviceComponentManager.getStateService());
			handler.setSandboxService(serviceComponentManager.getSandboxService());
			handler.setMemoryService(serviceComponentManager.getMemoryService());
			handler.setSessionHistoryService(serviceComponentManager.getSessionHistoryService());
			return handler;
		}
	}

	public static class MySandboxServiceProvider implements AgentApp.SandboxServiceProvider {
		@Override
		public SandboxService get(Properties properties) {
			BaseClientStarter clientConfig = DockerClientStarter.builder().build();
			ManagerConfig managerConfig = ManagerConfig.builder()
					.clientStarter(clientConfig)
					.build();
			return new SandboxService(managerConfig);
		}
	}
}
