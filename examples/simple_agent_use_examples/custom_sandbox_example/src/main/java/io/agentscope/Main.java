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

import com.google.gson.Gson;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

public class Main {
    public static void main(String[] args) {
        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        sandboxService.start();

        try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", CustomSandbox.class)) {
            Gson gson = new Gson();
            String tools = gson.toJson(sandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}