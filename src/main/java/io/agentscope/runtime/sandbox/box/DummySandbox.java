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
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Dummy Sandbox implementation
 * Corresponds to Python's DummySandbox
 * A lightweight sandbox for testing purposes with minimal functionality
 * 
 * <p>This is a special implementation that doesn't create real containers,
 * suitable for testing and development purposes.
 */
public class DummySandbox implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(DummySandbox.class.getName());
    
    private final String sandboxId;
    private final String userId;
    private final String sessionId;
    private final SandboxType sandboxType;
    private boolean closed = false;
    
    public DummySandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000);
    }
    
    public DummySandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.sandboxType = SandboxType.BASE;
        
        // For dummy sandbox, we just use a fixed ID
        this.sandboxId = "dummy-sandbox-" + userId + "-" + sessionId;
        
        logger.info("DummySandbox initialized: " + this.sandboxId + 
                   " (This is a dummy sandbox, no real container created)");
    }
    
    public String getSandboxId() {
        return sandboxId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public SandboxType getSandboxType() {
        return sandboxType;
    }
    
    public ContainerModel getInfo() {
        logger.info("DummySandbox.getInfo() called - returning dummy info");
        ContainerModel model = new ContainerModel();
        model.setContainerName(sandboxId);
        model.setSessionId(sessionId);
        return model;
    }
    
    public Map<String, Object> listTools(String toolType) {
        logger.info("DummySandbox.listTools() called - returning empty map");
        return new java.util.HashMap<>();
    }
    
    public String callTool(String name, Map<String, Object> arguments) {
        logger.info("DummySandbox.callTool() called with name=" + name + 
                   " - returning dummy response");
        return "{\"status\": \"success\", \"message\": \"Dummy sandbox call\"}";
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        logger.info("DummySandbox.close() called - no-op");
    }
    
    public void release() {
        logger.info("DummySandbox.release() called - no-op");
        close();
    }
    
    public boolean isClosed() {
        return closed;
    }
}

