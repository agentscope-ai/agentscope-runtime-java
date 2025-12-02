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
package io.agentscope.runtime.autoconfigure;

import io.agentscope.runtime.app.AgentApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

/**
 * Spring Boot lifecycle listener for AgentApp.
 * 
 * <p>This class corresponds to the lifespan manager in Python's FastAPIAppFactory.
 * It handles application startup and shutdown lifecycle events, calling the
 * registered lifecycle handlers in AgentApp.</p>
 * 
 * <p>Lifecycle flow (corresponds to Python's FastAPI lifespan):</p>
 * <ol>
 *   <li>On startup (ContextRefreshedEvent):
 *     <ul>
 *       <li>Call before_start callback (if registered)</li>
 *       <li>Call init_handler (if registered via app.init())</li>
 *       <li>Initialize and start the Runner</li>
 *     </ul>
 *   </li>
 *   <li>On shutdown (ContextClosedEvent):
 *     <ul>
 *       <li>Stop the Runner</li>
 *       <li>Call shutdown_handler (if registered via app.shutdown())</li>
 *       <li>Call after_finish callback (if registered)</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * <p>This listener is automatically registered when AgentApp is available as a Spring bean.</p>
 */
@Component
public class AgentAppLifecycleListener implements 
        ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentAppLifecycleListener.class);
    
    private final AgentApp agentApp;
    private boolean initialized = false;
    
    /**
     * Constructor with AgentApp dependency injection.
     * 
     * @param agentApp the AgentApp instance (optional, may be null if not configured)
     */
    @Autowired(required = false)
    public AgentAppLifecycleListener(AgentApp agentApp) {
        this.agentApp = agentApp;
    }
    
    /**
     * Handle application startup event.
     * 
     * <p>This corresponds to the startup phase in Python's FastAPI lifespan manager.
     * It calls before_start callback and init_handler, then initializes and starts the runner.</p>
     * 
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // Only handle the root context refresh event (avoid duplicate calls)
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        
        if (agentApp == null) {
            logger.debug("[AgentAppLifecycleListener] AgentApp not configured, skipping lifecycle management");
            return;
        }
        
        if (initialized) {
            logger.debug("[AgentAppLifecycleListener] Already initialized, skipping");
            return;
        }
        
        try {
            logger.info("[AgentAppLifecycleListener] Starting AgentApp lifecycle...");
            
            // Call before_start callback (corresponds to Python's before_start in lifespan)
            Runnable beforeStart = agentApp.getBeforeStart();
            if (beforeStart != null) {
                try {
                    logger.debug("[AgentAppLifecycleListener] Calling before_start callback");
                    beforeStart.run();
                } catch (Exception e) {
                    logger.warn("[AgentAppLifecycleListener] Exception in before_start callback: {}", 
                        e.getMessage(), e);
                }
            }
            
            // Initialize and start the runner (this will call init_handler if registered)
            // Corresponds to Python's runner.__aenter__() and init_handler call
            logger.debug("[AgentAppLifecycleListener] Initializing and starting runner");
            agentApp.init()
                .thenCompose(v -> agentApp.start())
                .thenRun(() -> {
                    logger.info("[AgentAppLifecycleListener] AgentApp started successfully");
                    initialized = true;
                })
                .exceptionally(ex -> {
                    logger.error("[AgentAppLifecycleListener] Failed to start AgentApp: {}", 
                        ex.getMessage(), ex);
                    return null;
                });
            
        } catch (Exception e) {
            logger.error("[AgentAppLifecycleListener] Error during startup: {}", 
                e.getMessage(), e);
        }
    }
}

/**
 * Separate listener for application shutdown events.
 * 
 * <p>This corresponds to the shutdown phase in Python's FastAPI lifespan manager.</p>
 */
@Component
class AgentAppShutdownListener implements ApplicationListener<ContextClosedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentAppShutdownListener.class);
    
    private final AgentApp agentApp;
    
    /**
     * Constructor with AgentApp dependency injection.
     * 
     * @param agentApp the AgentApp instance (optional, may be null if not configured)
     */
    @Autowired(required = false)
    public AgentAppShutdownListener(AgentApp agentApp) {
        this.agentApp = agentApp;
    }
    
    /**
     * Handle application shutdown event.
     * 
     * <p>This corresponds to the shutdown phase in Python's FastAPI lifespan manager.
     * It stops the runner, calls shutdown_handler, and then calls after_finish callback.</p>
     * 
     * @param event the context closed event
     */
    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        // Only handle the root context close event (avoid duplicate calls)
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        
        if (agentApp == null) {
            return;
        }
        
        try {
            logger.info("[AgentAppShutdownListener] Shutting down AgentApp...");
            
            // Stop the runner first (corresponds to Python's runner.__aexit__())
            agentApp.stop()
                .thenCompose(v -> {
                    // Call shutdown_handler (corresponds to Python's shutdown_handler call)
                    Runnable shutdownHandler = agentApp.getShutdownHandler();
                    if (shutdownHandler != null) {
                        try {
                            logger.debug("[AgentAppShutdownListener] Calling shutdown_handler");
                            shutdownHandler.run();
                        } catch (Exception e) {
                            logger.warn("[AgentAppShutdownListener] Exception in shutdown_handler: {}", 
                                e.getMessage(), e);
                        }
                    }
                    
                    // Call shutdown() which also handles shutdown_handler
                    return agentApp.shutdown();
                })
                .thenRun(() -> {
                    // Call after_finish callback (corresponds to Python's after_finish in lifespan)
                    Runnable afterFinish = agentApp.getAfterFinish();
                    if (afterFinish != null) {
                        try {
                            logger.debug("[AgentAppShutdownListener] Calling after_finish callback");
                            afterFinish.run();
                        } catch (Exception e) {
                            logger.warn("[AgentAppShutdownListener] Exception in after_finish callback: {}", 
                                e.getMessage(), e);
                        }
                    }
                    
                    logger.info("[AgentAppShutdownListener] AgentApp shutdown completed");
                })
                .exceptionally(ex -> {
                    logger.error("[AgentAppShutdownListener] Error during shutdown: {}", 
                        ex.getMessage(), ex);
                    return null;
                });
            
        } catch (Exception e) {
            logger.error("[AgentAppShutdownListener] Error during shutdown: {}", 
                e.getMessage(), e);
        }
    }
}

