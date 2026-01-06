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

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

import java.util.Objects;
import java.util.Properties;

public class AgentScopeDeployWithCommandLineExample {

    public static void main(String[] args) {
        String[] commandLine = new String[2];
        commandLine[0] = "-f";
        commandLine[1] = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(".env")).getPath();
        AgentApp app = new AgentApp(commandLine);
        app.run();
    }

    public static class MyAgentHandlerProvider implements AgentApp.AgentHandlerProvider {

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
                    .clientConfig(clientConfig)
                    .build();
            return new SandboxService(
                    managerConfig
            );
        }
    }
}
