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

/**
 * Abstract base class for services.
 * 
 * <p>This class defines the interface that all services must implement.
 * Services provide lifecycle management (start, stop, health check) and
 * can be used with try-with-resources pattern for automatic cleanup.
 * 
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * Service service = new MyService();
 * 
 * // Manual lifecycle management
 * service.start().join();
 * try {
 *     // Use service
 *     boolean healthy = service.health().join();
 * } finally {
 *     service.stop().join();
 * }
 * }</pre>
 */
public interface Service {
    
    /**
	 * Starts the service, initializing any necessary resources or connections.
	 */
    void start();
    
    /**
	 * Stops the service, releasing any acquired resources.
	 */
    void stop();
    
    /**
	 * Checks the health of the service.
	 *
	 * @return
	 */
    boolean health();
}

