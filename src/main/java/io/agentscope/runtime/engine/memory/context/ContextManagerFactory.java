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
package io.agentscope.runtime.engine.memory.context;

import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.service.RedisMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.RedisSessionHistoryService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Context manager factory class
 * Provides convenient methods to create ContextManager instances
 */
public class ContextManagerFactory {
    
    /**
     * Create default context manager (using memory implementation)
     *
     * @return ContextManager default context manager
     */
    public static ContextManager createDefault() {
        return new ContextManager();
    }
    
    /**
     * Create context manager using Redis
     *
     * @param redisTemplate Redis template
     * @return ContextManager context manager using Redis
     */
    public static ContextManager createRedis(RedisTemplate<String, String> redisTemplate) {
        MemoryService memoryService = new RedisMemoryService(redisTemplate);
        SessionHistoryService sessionHistoryService = new RedisSessionHistoryService(redisTemplate);
        
        return new ContextManager(
                ContextComposer.class,
                sessionHistoryService,
                memoryService
        );
    }
    
    /**
     * Create custom context manager
     *
     * @param memoryService memory service
     * @param sessionHistoryService session history service
     * @return ContextManager custom context manager
     */
    public static ContextManager createCustom(
            MemoryService memoryService,
            SessionHistoryService sessionHistoryService) {
        return new ContextManager(
                ContextComposer.class,
                sessionHistoryService,
                memoryService
        );
    }
    
    /**
     * Create context manager and start asynchronously
     *
     * @param memoryService optional memory service
     * @param sessionHistoryService optional session history service
     * @return CompletableFuture<ContextManager> asynchronously created context manager
     */
    public static CompletableFuture<ContextManager> createAndStartAsync(
            Optional<MemoryService> memoryService,
            Optional<SessionHistoryService> sessionHistoryService) {
        
        return CompletableFuture.supplyAsync(() -> {
            ContextManager manager = new ContextManager(
                    ContextComposer.class,
                    sessionHistoryService.orElse(null),
                    memoryService.orElse(null)
            );
            
            try {
                manager.start().get();
                return manager;
            } catch (Exception e) {
                throw new RuntimeException("Failed to start context manager", e);
            }
        });
    }
}
