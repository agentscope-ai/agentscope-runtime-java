package io.agentscope.runtime.engine.services.sandbox;

import io.agentscope.runtime.engine.services.ServiceWithLifecycleManager;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service for managing sandbox environments.
 *
 * <p>This service provides functionality to connect to sandbox environments,
 * create new environments, and release them. It manages the lifecycle of
 * sandbox instances associated with user sessions.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * SandboxService sandboxService = new SandboxService("http://localhost:8000", "token");
 * sandboxService.start().join();
 *
 * // Connect to sandbox for a session
 * List<SandboxInterface> sandboxes = sandboxService.connect("session_123", "user_456", null);
 *
 * // Release sandbox
 * sandboxService.release("session_123", "user_456");
 * }</pre>
 */
public class SandboxService extends ServiceWithLifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(SandboxService.class);

    private SandboxManager managerApi;
    private boolean health = false;

    public SandboxService(SandboxManager sandboxManager) {
       this.managerApi = sandboxManager;
    }

    @Override
    public CompletableFuture<Void> start() {
        health = true;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stop() {
        health = false;
        if (managerApi != null) {
            managerApi.close();
        }
        managerApi = null;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.completedFuture(health);
    }

    /**
     * Connect to sandbox environment for a session.
     *
     * @param sessionId The session ID
     * @param userId Optional user ID
     * @param sandboxType sandbox type to connect to
     * @return List of sandbox instances
     */
    public Sandbox connect(
            String sessionId,
            String userId,
            Class<? extends Sandbox> sandboxType) {
		try {
			return sandboxType.getConstructor(
					SandboxManager.class,
					String.class,
					String.class
			).newInstance(managerApi, userId, sessionId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}

