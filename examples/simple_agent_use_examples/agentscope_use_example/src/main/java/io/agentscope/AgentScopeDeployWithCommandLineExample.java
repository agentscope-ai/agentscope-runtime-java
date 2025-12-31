package io.agentscope;

import java.util.Objects;
import java.util.Properties;

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

public class AgentScopeDeployWithCommandLineExample {

	public static void main(String[] args) {
		String[] commandLine = new String[2];
		commandLine[0] = "-f";
		commandLine[1] = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(".env")).getPath();
		AgentApp app = new AgentApp(commandLine);
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
			BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();
			ManagerConfig managerConfig = ManagerConfig.builder()
					.containerDeployment(clientConfig)
					.build();
			return new SandboxService(
					new SandboxManager(managerConfig));
		}
	}
}
