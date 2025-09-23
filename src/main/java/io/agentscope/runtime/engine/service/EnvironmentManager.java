package io.agentscope.runtime.engine.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Environment manager interface
 * Corresponds to the EnvironmentManager class in environment_manager.py of the Python version
 */
public interface EnvironmentManager {
    
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
