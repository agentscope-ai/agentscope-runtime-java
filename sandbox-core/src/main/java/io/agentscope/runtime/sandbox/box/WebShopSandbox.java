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
package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemConfig;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import java.util.Map;

@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest",
        sandboxType = "webshop",
        runtimeConfig = {"shm_size=5.06gb"},
        securityLevel = "medium",
        timeout = 30,
        description = "webshop Sandbox"
)
public class WebShopSandbox extends TrainingSandbox {
    public WebShopSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId) {
        this(managerApi, userId, sessionId, Map.of());
    }

    public WebShopSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemConfig fileSystemConfig) {
        this(managerApi, userId, sessionId, fileSystemConfig, Map.of());
    }

    public WebShopSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment) {
        this(managerApi, userId, sessionId, LocalFileSystemConfig.builder().build(), environment);
    }

    public WebShopSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemConfig fileSystemConfig,
            Map<String, String> environment
    ) {
        super(managerApi, userId, sessionId, "webshop", fileSystemConfig, environment);
    }
}

