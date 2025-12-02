package io.agentscope.runtime.engine.services;

import java.util.concurrent.CompletableFuture;

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
     * 
     * @return A CompletableFuture that completes when the service has started
     */
    CompletableFuture<Void> start();
    
    /**
     * Stops the service, releasing any acquired resources.
     * 
     * @return A CompletableFuture that completes when the service has stopped
     */
    CompletableFuture<Void> stop();
    
    /**
     * Checks the health of the service.
     * 
     * @return A CompletableFuture that completes with true if the service is healthy,
     *         false otherwise
     */
    CompletableFuture<Boolean> health();
}

