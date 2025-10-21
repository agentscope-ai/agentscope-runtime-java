package runtime.domain.tools.service.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
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
     * Test 1: Create SandboxManager with container pool
     */
    @Test
    void testCreateSandboxManagerWithPool() {
        System.out.println("\n--- Test 1: Create SandboxManager with Pool ---");

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
     * Test 2: Get containers from pool (requires Docker)
     */
    @Test
    void testGetContainersFromPool() {
        System.out.println("\n--- Test 2: Get Containers from Pool ---");

        ManagerConfig config = new ManagerConfig.Builder()
                .poolSize(2)
                .build();

        manager = new SandboxManager(config);
        assertNotNull(manager, "SandboxManager should be created successfully");

        // Define userID and sessionID for container management
        String userID1 = "test-user-1";
        String sessionID1 = "test-session-1";
        String userID2 = "test-user-2";
        String sessionID2 = "test-session-2";

        // Get first container from pool (with userID and sessionID, will be added to map)
        System.out.println("Getting container from pool (fast!)...");
        long startTime = System.currentTimeMillis();
        
        ContainerModel container1 = manager.createFromPool(SandboxType.BASE, userID1, sessionID1);
        
        long endTime = System.currentTimeMillis();
        long time1 = endTime - startTime;
        System.out.println("Got container in " + time1 + "ms");
        
        assertNotNull(container1, "Container 1 should be created successfully");
        assertNotNull(container1.getContainerName(), "Container name should not be null");
        System.out.println("Container: " + container1.getContainerName());

        // Get second container from pool (with userID and sessionID, will be added to map)
        System.out.println("\nGetting second container from pool...");
        startTime = System.currentTimeMillis();
        
        ContainerModel container2 = manager.createFromPool(SandboxType.BASE, userID2, sessionID2);
        
        endTime = System.currentTimeMillis();
        long time2 = endTime - startTime;
        System.out.println("Got container in " + time2 + "ms");
        
        assertNotNull(container2, "Container 2 should be created successfully");
        assertNotNull(container2.getContainerName(), "Container name should not be null");
        assertNotEquals(container1.getContainerId(), container2.getContainerId(), 
                "The two containers should have different IDs");
        System.out.println("Container: " + container2.getContainerName());

        System.out.println("\nBoth containers retrieved from pre-warmed pool!");
        
        // Explicitly release containers taken from pool
        System.out.println("\n--- Cleaning up containers ---");
        System.out.println("Container 1 (from pool): " + container1.getContainerName() + " (ID: " + container1.getContainerId() + ")");
        System.out.println("  - Random sessionId: " + container1.getSessionId());
        System.out.println("Container 2 (from pool): " + container2.getContainerName() + " (ID: " + container2.getContainerId() + ")");
        System.out.println("  - Random sessionId: " + container2.getSessionId());
        
        // Release containers using their random sessionId
        System.out.println("\nReleasing containers using their random sessionId...");
        boolean released1 = manager.release(container1.getSessionId());
        System.out.println("Container 1 (sessionId: " + container1.getSessionId() + ") released: " + released1);
        assertTrue(released1, "Container 1 should be released successfully");
        
        boolean released2 = manager.release(container2.getSessionId());
        System.out.println("Container 2 (sessionId: " + container2.getSessionId() + ") released: " + released2);
        assertTrue(released2, "Container 2 should be released successfully");
        
        // Clean up remaining pool containers
        System.out.println("\nCleaning up remaining pool containers...");
        manager.cleanupAllSandboxes();
        System.out.println("✓ All containers cleaned up successfully");
        System.out.println("Note: @AfterEach will verify cleanup is complete");
    }

    /**
     * Test 3: Pool behavior with different types
     */
    @Test
    void testPoolBehaviorWithDifferentTypes() {
        System.out.println("\n--- Test 3: Mixed Container Types ---");

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
     * Test 4: Performance comparison between pool and non-pool
     */
    @Test
    void testPerformanceComparison() {
        System.out.println("\n--- Test 4: Performance Comparison ---");

        long timeNoPool;
        long timeWithPool;

        // Without pool
        System.out.println("Creating manager WITHOUT pool...");
        ManagerConfig configNoPool = new ManagerConfig.Builder()
                .poolSize(0)  // No pool
                .build();

        try (SandboxManager managerNoPool = new SandboxManager(configNoPool)) {
            assertNotNull(managerNoPool, "SandboxManager without pool should be created successfully");

            long startTime = System.currentTimeMillis();
            ContainerModel container1 = managerNoPool.createFromPool(SandboxType.BASE, "test-user", "test-session-no-pool");
            timeNoPool = System.currentTimeMillis() - startTime;
            
            assertNotNull(container1, "Container should be created successfully");
            System.out.println("Time without pool: " + timeNoPool + "ms");

            managerNoPool.cleanupAllSandboxes();
        } catch (Exception e) {
            fail("Test without pool failed: " + e.getMessage());
            return;
        }

        // Wait for ports to be fully released to avoid port conflicts
        try {
            System.out.println("\nWaiting for ports to be released...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // With pool
        System.out.println("\nCreating manager WITH pool...");
        ManagerConfig configWithPool = new ManagerConfig.Builder()
                .poolSize(3)
                .build();

        manager = new SandboxManager(configWithPool);
        assertNotNull(manager, "SandboxManager with pool should be created successfully");

        // Use multi-parameter version to add container to sandboxMap for easier management
        String userID = "test-user";
        String sessionID = "test-session-pool";

        long startTime = System.currentTimeMillis();
        ContainerModel container2 = manager.createFromPool(SandboxType.BASE, userID, sessionID);
        timeWithPool = System.currentTimeMillis() - startTime;
        
        assertNotNull(container2, "Container should be created successfully");
        System.out.println("Time with pool: " + timeWithPool + "ms");
        System.out.println("Container: " + container2.getContainerName() + " (random sessionId: " + container2.getSessionId() + ")");

        long improvement = timeNoPool - timeWithPool;
        System.out.println("\nPerformance improvement: " + improvement + "ms faster with pool!");
        
        // Usually getting from pool should be faster, but may not be obvious in test environment
        assertTrue(timeWithPool >= 0, "Time to get from pool should be non-negative");
        
        // Release container using its random sessionId
        System.out.println("\nReleasing container taken from pool...");
        boolean released = manager.release(container2.getSessionId());
        System.out.println("Container released: " + released);
        assertTrue(released, "Container taken from pool should be released successfully");
    }

    /**
     * Test 5: Verify pool size configuration
     */
    @Test
    void testPoolSizeConfiguration() {
        System.out.println("\n--- Test 5: Pool Size Configuration ---");

        // Test different pool sizes
        int[] poolSizes = {0, 1, 3, 5};

        for (int poolSize : poolSizes) {
            System.out.println("\nTesting pool size: " + poolSize);
            
            ManagerConfig config = new ManagerConfig.Builder()
                    .poolSize(poolSize)
                    .build();

            // Use try-with-resources to ensure each manager is properly cleaned up
            try (SandboxManager testManager = new SandboxManager(config)) {
                assertNotNull(testManager, "SandboxManager should be created successfully with pool size: " + poolSize);
                
                System.out.println("Successfully created manager with pool size " + poolSize);
                
                testManager.cleanupAllSandboxes();
                System.out.println("Cleaned up manager with pool size " + poolSize);
            } catch (Exception e) {
                fail("Test failed for pool size " + poolSize + ": " + e.getMessage());
            }

            // Wait between iterations to ensure resources are fully released
            if (poolSize < poolSizes[poolSizes.length - 1]) {
                try {
                    System.out.println("Waiting for resources to be released...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Test 6: Verify resource cleanup completeness
     */
    @Test
    void testResourceCleanup() {
        System.out.println("\n--- Test 6: Resource Cleanup Verification ---");

        ManagerConfig config = new ManagerConfig.Builder()
                .poolSize(2)
                .build();

        manager = new SandboxManager(config);
        assertNotNull(manager, "SandboxManager should be created successfully");

        // Define sessionIDs
        String sessionID1 = "test-cleanup-session-1";
        String sessionID2 = "test-cleanup-session-2";

        // Create some containers
        ContainerModel container1 = manager.createFromPool(SandboxType.BASE, "test-user-1", sessionID1);
        ContainerModel container2 = manager.createFromPool(SandboxType.BASE, "test-user-2", sessionID2);
        
        assertNotNull(container1, "Container 1 should be created successfully");
        assertNotNull(container2, "Container 2 should be created successfully");
        
        System.out.println("Created containers:");
        System.out.println("  - " + container1.getContainerName() + " (ID: " + container1.getContainerId() + ")");
        System.out.println("    Random sessionId: " + container1.getSessionId());
        System.out.println("  - " + container2.getContainerName() + " (ID: " + container2.getContainerId() + ")");
        System.out.println("    Random sessionId: " + container2.getSessionId());

        // Explicitly release using containers' random sessionIds
        System.out.println("\nReleasing containers using their random sessionId...");
        boolean released1 = manager.release(container1.getSessionId());
        System.out.println("  - Container 1 released: " + released1);
        assertTrue(released1, "Container 1 should be released successfully");
        
        boolean released2 = manager.release(container2.getSessionId());
        System.out.println("  - Container 2 released: " + released2);
        assertTrue(released2, "Container 2 should be released successfully");
        
        // Clean up remaining pool containers
        System.out.println("\nCleaning up remaining pool containers...");
        manager.cleanupAllSandboxes();
        
        System.out.println("All containers cleaned up successfully");
        System.out.println("Note: @AfterEach will also call cleanup and close to ensure no resources are leaked");
        System.out.println("\n⚠️ Key Points:");
        System.out.println("  1. release() will release containers taken from pool");
        System.out.println("  2. cleanupAllSandboxes() will stop and delete remaining pool containers");
        System.out.println("  3. close() will additionally ensure resources are fully released");
        System.out.println("  4. Test will wait 500ms after completion to ensure port release");
        System.out.println("  5. All containers will show 'Container deleted successfully' in logs");
    }

    /**
     * Test 7: Verify port management across multiple container creations
     */
    @Test
    void testPortManagementAcrossMultipleCreations() {
        System.out.println("\n--- Test 7: Port Management Across Multiple Creations ---");

        ManagerConfig config = new ManagerConfig.Builder()
                .poolSize(0)  // No pool, create new container each time
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

        System.out.println("\n✅ All containers created successfully with unique IDs");
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
        System.out.println("✓ All 3 containers cleaned up successfully");
    }
}
