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

