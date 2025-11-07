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

package io.agentscope.runtime.engine.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.agentscope.runtime.sandbox.manager.SandboxManager;

/**
 * Environment manager interface
 * Corresponds to the EnvironmentManager class in environment_manager.py of the Python version
 */
public interface EnvironmentManager {

    SandboxManager getSandboxManager();
    
    /**
     * Get environment variable
     */
    String getEnvironmentVariable(String key);
    
    /**
     * Set environment variable
     */
    void setEnvironmentVariable(String key, String value);
    
    /**
     * Get all environment variables
     */
    Map<String, String> getAllEnvironmentVariables();
    
    /**
     * Check if environment is available
     */
    boolean isEnvironmentAvailable();
    
    /**
     * Initialize environment
     */
    CompletableFuture<Void> initializeEnvironment();
    
    /**
     * Clean up environment
     */
    CompletableFuture<Void> cleanupEnvironment();
    
    /**
     * Get environment information
     */
    Map<String, Object> getEnvironmentInfo();
}
