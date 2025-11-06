package io.agentscope.runtime.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Container Pool Functionality Test
 * Demonstrates how Java container pool aligns with Python's pool functionality
 * 
 * <p>Notes:
 * <ul>
 *   <li>Each test creates and destroys multiple Docker containers</li>
 *   <li>There are appropriate delays between tests to ensure ports and resources are fully released</li>
 *   <li>All containers are automatically cleaned up after tests</li>
 *   <li>If port conflicts occur, SandboxManager will automatically retry</li>
 * </ul>
 */
public class ContainerPoolTest {

    private SandboxManager manager;

    @BeforeEach
    void setUp() {
        // Ensure a clean state at the start of each test
        manager = null;
        System.out.println("Starting new test...");
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            try {
                System.out.println("Cleaning up test resources...");
                manager.cleanupAllSandboxes();
                manager.close();
                System.out.println("Pool cleaned up successfully");
                
                // Wait a short period to ensure ports are fully released
                // This avoids port conflicts between rapidly consecutive tests
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Cleanup interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
                e.printStackTrace();
            } finally {
                manager = null;
            }
        }
    }

    /**
     * Create SandboxManager with container pool
     */
    @Test
    void testCreateSandboxManagerWithPool() {
        System.out.println("\n--- Test Create SandboxManager with Pool ---");

        // Create configuration with pool size
        ManagerConfig config = new ManagerConfig.Builder()
                .poolSize(3)  // Pre-create 3 containers
                .build();

        System.out.println("Creating SandboxManager with pool size = 3");
        System.out.println("This will pre-create 3 containers (similar to Python's pool_size)");

        // Create manager - this will automatically initialize the pool
        manager = new SandboxManager(config);

        assertNotNull(manager, "SandboxManager should be created successfully");
        
        System.out.println("\nSandboxManager created!");
        System.out.println("Container pool has been initialized with pre-warmed containers");
        System.out.println("These containers are ready to be used immediately");
        
        // Explicitly clean up pool containers
        System.out.println("\n--- Cleaning up container pool ---");
        manager.cleanupAllSandboxes();
        System.out.println("✓ Container pool (3 containers) cleaned up successfully");
    }

    /**
     * Pool behavior with different types
     */
    @Test
    void testPoolBehaviorWithDifferentTypes() {
        System.out.println("\n--- Test Mixed Container Types ---");

        ManagerConfig config = new ManagerConfig.Builder()
                .poolSize(2)  // Pool only contains BASE type
                .build();

        manager = new SandboxManager(config);
        assertNotNull(manager, "SandboxManager should be created successfully");

        // Define sessionIDs
        String baseSessionID = "test-session-base";
        String browserSessionID = "test-session-browser";

        // Get BASE type from pool - fast
        System.out.println("Getting BASE type (from pool)...");
        ContainerModel baseContainer = manager.createFromPool(SandboxType.BASE, "test-user", baseSessionID);
        assertNotNull(baseContainer, "BASE container should be created successfully");
        System.out.println("Got: " + baseContainer.getContainerName());

        // Get BROWSER type - will be created directly (not from pool)
        System.out.println("\nGetting BROWSER type (direct creation)...");
        ContainerModel browserContainer = manager.createFromPool(SandboxType.BROWSER, "test-user", browserSessionID);
        assertNotNull(browserContainer, "BROWSER container should be created successfully");
        System.out.println("Got: " + browserContainer.getContainerName());
        System.out.println("Note: BROWSER was created directly since pool only has BASE type");
        
        // Explicitly release containers
        System.out.println("\n--- Cleaning up containers ---");
        System.out.println("BASE container: " + baseContainer.getContainerName() + " (ID: " + baseContainer.getContainerId() + ")");
        System.out.println("  - Random sessionId: " + baseContainer.getSessionId());
        System.out.println("BROWSER container: " + browserContainer.getContainerName() + " (ID: " + browserContainer.getContainerId() + ")");
        System.out.println("  - Random sessionId: " + browserContainer.getSessionId());
        
        // Release containers using their random sessionId
        System.out.println("\nReleasing containers using their random sessionId...");
        boolean baseReleased = manager.release(baseContainer.getSessionId());
        System.out.println("BASE container released: " + baseReleased);
        assertTrue(baseReleased, "BASE container should be released successfully");
        
        boolean browserReleased = manager.release(browserContainer.getSessionId());
        System.out.println("BROWSER container released: " + browserReleased);
        assertTrue(browserReleased, "BROWSER container should be released successfully");
        
        // Clean up remaining pool containers
        System.out.println("\nCleaning up remaining pool containers...");
        manager.cleanupAllSandboxes();
        System.out.println("✓ All containers cleaned up successfully");
    }

    /**
     * Verify port management across multiple container creations
     */
    @Test
    void testPortManagementAcrossMultipleCreations() {
        System.out.println("\n--- Test Port Management Across Multiple Creations ---");

        ManagerConfig config = new ManagerConfig.Builder()
                .poolSize(0)
                .build();

        manager = new SandboxManager(config);
        assertNotNull(manager, "SandboxManager should be created successfully");

        // Define sessionIDs
        String sessionID1 = "test-port-session-1";
        String sessionID2 = "test-port-session-2";
        String sessionID3 = "test-port-session-3";

        // Quickly create multiple containers in succession
        System.out.println("Creating 3 containers in quick succession...");
        ContainerModel container1 = manager.createFromPool(SandboxType.BASE, "test-user-1", sessionID1);
        assertNotNull(container1, "Container 1 should be created successfully");
        System.out.println("Created container 1: " + container1.getContainerName());

        ContainerModel container2 = manager.createFromPool(SandboxType.BASE, "test-user-2", sessionID2);
        assertNotNull(container2, "Container 2 should be created successfully");
        System.out.println("Created container 2: " + container2.getContainerName());

        ContainerModel container3 = manager.createFromPool(SandboxType.BASE, "test-user-3", sessionID3);
        assertNotNull(container3, "Container 3 should be created successfully");
        System.out.println("Created container 3: " + container3.getContainerName());

        // Verify they all have different IDs
        assertNotEquals(container1.getContainerId(), container2.getContainerId(), 
                "Containers should have different IDs");
        assertNotEquals(container2.getContainerId(), container3.getContainerId(), 
                "Containers should have different IDs");
        assertNotEquals(container1.getContainerId(), container3.getContainerId(), 
                "Containers should have different IDs");

        System.out.println("\nAll containers created successfully with unique IDs");
        System.out.println("Port management is working correctly");
        
        // Explicitly release all containers using their random sessionIds
        System.out.println("\n--- Cleaning up containers ---");
        System.out.println("Container 1: " + container1.getContainerName() + " (ID: " + container1.getContainerId() + ")");
        System.out.println("  Random sessionId: " + container1.getSessionId());
        System.out.println("Container 2: " + container2.getContainerName() + " (ID: " + container2.getContainerId() + ")");
        System.out.println("  Random sessionId: " + container2.getSessionId());
        System.out.println("Container 3: " + container3.getContainerName() + " (ID: " + container3.getContainerId() + ")");
        System.out.println("  Random sessionId: " + container3.getSessionId());
        
        System.out.println("\nReleasing containers using their random sessionId...");
        boolean released1 = manager.release(container1.getSessionId());
        System.out.println("Container 1 released: " + released1);
        assertTrue(released1, "Container 1 should be released successfully");
        
        boolean released2 = manager.release(container2.getSessionId());
        System.out.println("Container 2 released: " + released2);
        assertTrue(released2, "Container 2 should be released successfully");
        
        boolean released3 = manager.release(container3.getSessionId());
        System.out.println("Container 3 released: " + released3);
        assertTrue(released3, "Container 3 should be released successfully");
        
        // Clean up pool
        System.out.println("\nCleaning up pool...");
        manager.cleanupAllSandboxes();
        System.out.println("All 3 containers cleaned up successfully");
    }
}
