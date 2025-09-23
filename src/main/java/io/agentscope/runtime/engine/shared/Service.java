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
package io.agentscope.runtime.engine.shared;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface that defines basic methods that all services must implement
 */
public interface Service {
    
    /**
     * Start the service, initialize necessary resources or connections
     *
     * @return CompletableFuture<Void> asynchronous startup result
     */
    CompletableFuture<Void> start();
    
    /**
     * Stop the service, release acquired resources
     *
     * @return CompletableFuture<Void> asynchronous stop result
     */
    CompletableFuture<Void> stop();
    
    /**
     * Check the health status of the service
     *
     * @return CompletableFuture<Boolean> asynchronous health check result, true indicates healthy, false indicates unhealthy
     */
    CompletableFuture<Boolean> health();
}
