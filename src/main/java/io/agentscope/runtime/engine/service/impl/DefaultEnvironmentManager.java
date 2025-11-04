package io.agentscope.runtime.engine.service.impl;

import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Default environment manager implementation
 * Corresponds to the default implementation in environment_manager.py of the Python version
 */
public class DefaultEnvironmentManager implements EnvironmentManager {

    private final SandboxManager sandboxManager;
    private final Map<String, String> environmentVariables;
    private boolean initialized = false;
    
    public DefaultEnvironmentManager() {
        this.environmentVariables = new HashMap<>();
        this.sandboxManager = new SandboxManager();
    }

    public DefaultEnvironmentManager(SandboxManager sandboxManager) {
        this.environmentVariables = new HashMap<>();
        this.sandboxManager = sandboxManager;
    }

    @Override
    public SandboxManager getSandboxManager() {
        return sandboxManager;
    }

    @Override
    public String getEnvironmentVariable(String key) {
        // First get from system environment variables
        String systemValue = System.getenv(key);
        if (systemValue != null) {
            return systemValue;
        }
        
        // Then get from local storage
        return environmentVariables.get(key);
    }
    
    @Override
    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }
    
    @Override
    public Map<String, String> getAllEnvironmentVariables() {
        Map<String, String> allVars = new HashMap<>();
        
        // Add system environment variables
        allVars.putAll(System.getenv());
        
        // Add local environment variables
        allVars.putAll(environmentVariables);
        
        return allVars;
    }
    
    @Override
    public boolean isEnvironmentAvailable() {
        return initialized;
    }
    
    @Override
    public CompletableFuture<Void> initializeEnvironment() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Initialize environment
                // Specific initialization logic can be added here
                // For example, check necessary environment variables, create necessary directories, etc.

                // Set some default environment variables
                if (!environmentVariables.containsKey("AGENT_RUNTIME_HOME")) {
                    environmentVariables.put("AGENT_RUNTIME_HOME", System.getProperty("user.home") + "/.agent-runtime");
                }
                
                if (!environmentVariables.containsKey("AGENT_LOG_LEVEL")) {
                    environmentVariables.put("AGENT_LOG_LEVEL", "INFO");
                }
                
                initialized = true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize environment", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> cleanupEnvironment() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Clean up environment
                // Specific cleanup logic can be added here
                environmentVariables.clear();
                initialized = false;
            } catch (Exception e) {
                throw new RuntimeException("Failed to cleanup environment", e);
            }
        });
    }
    
    @Override
    public Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("initialized", initialized);
        info.put("environment_variables_count", environmentVariables.size());
        info.put("system_properties", System.getProperties());
        info.put("java_version", System.getProperty("java.version"));
        info.put("os_name", System.getProperty("os.name"));
        info.put("os_version", System.getProperty("os.version"));
        return info;
    }
}
