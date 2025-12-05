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

import io.agentscope.runtime.engine.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

/**
 * Spring Boot lifecycle listener for Runner.
 * 
 * It handles application startup and shutdown lifecycle events, calling the
 * registered lifecycle handlers in Runner.</p>
 * 
 * <p>Lifecycle flow:</p>
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
 * <p>This listener is automatically registered when Runner is available as a Spring bean.</p>
 */
@Component
public class AgentAppLifecycleListener implements 
        ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentAppLifecycleListener.class);
    
    private final Runner runner;
    private boolean initialized = false;
    
    /**
     * Constructor with Runner dependency injection.
     * 
     * @param runner the Runner instance (optional, may be null if not configured)
     */
    @Autowired(required = false)
    public AgentAppLifecycleListener(Runner runner) {
        this.runner = runner;
    }
    
    /**
     * Handle application startup event.
     * 
     * <p>It calls before_start callback and init_handler, then initializes and starts the runner.</p>
     * 
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // Only handle the root context refresh event (avoid duplicate calls)
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        
        if (runner == null) {
            logger.debug("[AgentAppLifecycleListener] Runner not configured, skipping lifecycle management");
            return;
        }
        
        if (initialized) {
            logger.debug("[AgentAppLifecycleListener] Already initialized, skipping");
            return;
        }
        
        try {
            logger.info("[AgentAppLifecycleListener] Starting Runner lifecycle...");
            
            // Initialize and start the runner (this will call init_handler if registered)
            logger.debug("[AgentAppLifecycleListener] Initializing and starting runner");
            try {
                runner.init();
                runner.start();
                initialized = true;
                logger.info("[AgentAppLifecycleListener] Runner started successfully");
            } catch (Exception e) {
                logger.error("[AgentAppLifecycleListener] Failed to start Runner: {}", e.getMessage());
                throw e;
            }

            
        } catch (Exception e) {
            logger.error("[AgentAppLifecycleListener] Error during startup: {}", 
                e.getMessage(), e);
        }
    }
}

/**
 * Separate listener for application shutdown events.
 * 
 */
@Component
class AgentAppShutdownListener implements ApplicationListener<ContextClosedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentAppShutdownListener.class);
    
    private final Runner runner;
    
    /**
     * Constructor with Runner dependency injection.
     * 
     * @param runner the Runner instance (optional, may be null if not configured)
     */
    @Autowired(required = false)
    public AgentAppShutdownListener(Runner runner) {
        this.runner = runner;
    }
    
    /**
     * Handle application shutdown event.
     * 
     * <p>It stops the runner, calls shutdown_handler, and then calls after_finish callback.</p>
     * 
     * @param event the context closed event
     */
    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        // Only handle the root context close event (avoid duplicate calls)
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        
        if (runner == null) {
            return;
        }
        
        try {
            logger.info("[AgentAppShutdownListener] Shutting down Runner...");
            
            // Stop the runner first
            try {
                runner.stop();
                runner.shutdown();
                logger.info("[AgentAppShutdownListener] Runner shutdown completed");
            } catch (Exception e) {
                logger.error("[AgentAppShutdownListener] Error during shutdown: {}", e.getMessage());
                throw e;
            }
            
        } catch (Exception e) {
            logger.error("[AgentAppShutdownListener] Error during shutdown: {}", 
                e.getMessage(), e);
        }
    }
}

