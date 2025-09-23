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

import io.agentscope.runtime.engine.memory.context.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Memory service startup listener
 * Automatically starts the memory service after application startup is complete
 */
@Component
@ConditionalOnProperty(name = "memory.service.auto-start", havingValue = "true", matchIfMissing = true)
public class MemoryServiceStartupListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryServiceStartupListener.class);
    
    @Autowired
    private ContextManager contextManager;
    
    @Autowired
    private MemoryProperties properties;
    
    /**
     * Automatically start memory service after application startup is complete
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application startup complete, starting memory service...");

        CompletableFuture.runAsync(() -> {
            try {
                // Check if the service is already started
                if (contextManager.hasService("memory") || contextManager.hasService("session")) {
                    logger.info("Memory service is already started, skipping auto-start");
                    return;
                }
                
                // Start memory service
                contextManager.start().get();
                logger.info("Memory service started successfully");

                // Execute health check (if enabled in configuration)
                if (properties.isHealthCheckOnStart()) {
                    contextManager.healthCheck().thenAccept(healthStatus -> {
                        logger.info("Memory service health check result: {}", healthStatus);
                        boolean allHealthy = healthStatus.values().stream().allMatch(Boolean::booleanValue);
                        if (allHealthy) {
                            logger.info("All memory service components are running normally");
                        } else {
                            logger.warn("Some memory service components are running abnormally: {}", healthStatus);
                        }
                    }).exceptionally(throwable -> {
                        logger.error("Memory service health check failed", throwable);
                        return null;
                    });
                }
                
            } catch (Exception e) {
                logger.error("Memory service startup failed", e);
                logger.warn("Memory service startup failed, but the application will continue to run. Please check the configuration or start the service manually.");
            }
        });
    }
}
