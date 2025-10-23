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
package io.agentscope.runtime.sandbox.manager.registry;

import io.agentscope.runtime.sandbox.box.*;

import java.util.logging.Logger;

/**
 * Sandbox registry initializer
 * Automatically registers all Sandbox classes with @RegisterSandbox annotation at application startup
 * 
 * <p>Usage:
 * <pre>{@code
 * // Call at application startup
 * SandboxRegistryInitializer.initialize();
 * }</pre>
 * 
 * <p>Or use Spring Boot's @PostConstruct annotation:
 * <pre>{@code
 * @Configuration
 * public class SandboxConfig {
 *     @PostConstruct
 *     public void init() {
 *         SandboxRegistryInitializer.initialize();
 *     }
 * }
 * }</pre>
 */
public class SandboxRegistryInitializer {
    private static final Logger logger = Logger.getLogger(SandboxRegistryInitializer.class.getName());
    private static boolean initialized = false;

    static{
        initialize();
    }
    
    /**
     * Initialize sandbox registry
     * Scan and register all classes with @RegisterSandbox annotation
     */
    public static synchronized void initialize() {
        if (initialized) {
            logger.info("SandboxRegistryService already initialized, skipping");
            return;
        }
        
        logger.info("Initializing SandboxRegistryService with annotated classes...");
        
        try {
            registerBuiltInSandboxes();
            initialized = true;
            logger.info("SandboxRegistryService initialization completed successfully");
            logRegistrationStats();
        } catch (Exception e) {
            logger.severe("Failed to initialize SandboxRegistryService: " + e.getMessage());
            throw new RuntimeException("Failed to initialize SandboxRegistryService", e);
        }
    }
    
    /**
     * Register built-in Sandbox classes
     */
    private static void registerBuiltInSandboxes() {
        logger.info("Registering built-in sandbox classes...");
        tryRegisterClass(BaseSandbox.class.getName());
        tryRegisterClass(FilesystemSandbox.class.getName());
        tryRegisterClass(BrowserSandbox.class.getName());
        tryRegisterClass(BFCLSandbox.class.getName());
        tryRegisterClass(WebShopSandbox.class.getName());
        tryRegisterClass(APPWorldSandbox.class.getName());
        logger.info("Built-in sandbox classes registered");
    }
    
    /**
     * Try to register the specified class
     * 
     * @param className Fully qualified class name
     */
    private static void tryRegisterClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            SandboxAnnotationProcessor.processClass(clazz);
        } catch (ClassNotFoundException e) {
            logger.fine("Sandbox class not found, skipping: " + className);
        } catch (Exception e) {
            logger.warning("Failed to register sandbox class " + className + ": " + e.getMessage());
        }
    }
    
    /**
     * Log registration statistics
     */
    private static void logRegistrationStats() {
        int standardCount = SandboxRegistryService.listAllSandboxesByType().size();
        int customCount = SandboxRegistryService.listAllCustomTypes().size();
        int totalCount = standardCount + customCount;
        
        logger.info("===== Sandbox Registration Statistics =====");
        logger.info("Standard sandbox types: " + standardCount);
        logger.info("Custom sandbox types: " + customCount);
        logger.info("Total registered sandboxes: " + totalCount);
        logger.info("==========================================");
        
        if (standardCount > 0) {
            logger.info("Registered standard sandboxes:");
            SandboxRegistryService.listAllSandboxesByType().forEach((type, config) -> {
                logger.info("  - " + type.getTypeName() + ": " + config.getImageName());
            });
        }
        
        if (customCount > 0) {
            logger.info("Registered custom sandboxes:");
            SandboxRegistryService.listAllCustomTypes().forEach((name, config) -> {
                logger.info("  - " + name + ": " + config.getImageName());
            });
        }
    }
    
    /**
     * Reset initialization state (mainly for testing)
     */
    public static synchronized void reset() {
        initialized = false;
        logger.info("SandboxRegistryService initialization state reset");
    }
    
    /**
     * Check if already initialized
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
