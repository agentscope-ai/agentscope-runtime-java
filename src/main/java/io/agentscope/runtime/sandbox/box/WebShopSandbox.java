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

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

/**
 * WebShop Sandbox implementation
 * Corresponds to Python's WebShopSandbox
 * Training Sandbox class for managing and executing webshop training-related tasks
 * 
 * <p>This class provides methods to create, manage, and interact with
 * webshop training environment instances using specialized tool calls.
 */
@RegisterSandbox(
    imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest",
    sandboxType = SandboxType.WEBSHOP,
    runtimeConfig = {"shm_size=5.06gb"},
    securityLevel = "medium",
    timeout = 30,
    description = "webshop Sandbox"
)
public class WebShopSandbox extends TrainingSandbox {
    
    public WebShopSandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000);
    }
    
    public WebShopSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        super(managerApi, userId, sessionId, SandboxType.WEBSHOP, timeout);
    }
}

