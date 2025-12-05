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

package io.agentscope.runtime.engine.services;

import java.util.concurrent.CompletableFuture;

/**
 * Base class for services that want lifecycle manager functionality.
 * 
 * <p>This class combines the Service interface with default implementations
 * for common lifecycle operations, providing a convenient base class for
 * most service implementations.
 * 
 * <p>Note: This is an abstract base class. Subclasses must implement the
 * abstract methods from the Service interface.
 * 
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * public class MyService extends ServiceWithLifecycleManager {
 *     @Override
 *     public CompletableFuture<Void> start() {
 *         // Initialize resources
 *         return CompletableFuture.completedFuture(null);
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Void> stop() {
 *         // Cleanup resources
 *         return CompletableFuture.completedFuture(null);
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Boolean> health() {
 *         return CompletableFuture.completedFuture(true);
 *     }
 * }
 * }</pre>
 */
public abstract class ServiceWithLifecycleManager implements Service {
    
    /**
     * Starts the service, initializing any necessary resources or connections.
     * 
     * @return A CompletableFuture that completes when the service has started
     */
    @Override
    public abstract CompletableFuture<Void> start();
    
    /**
     * Stops the service, releasing any acquired resources.
     * 
     * @return A CompletableFuture that completes when the service has stopped
     */
    @Override
    public abstract CompletableFuture<Void> stop();
    
    /**
     * Checks the health of the service.
     * 
     * @return A CompletableFuture that completes with true if the service is healthy,
     *         false otherwise
     */
    @Override
    public abstract CompletableFuture<Boolean> health();
}

