package runtime.domain.tools.service.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.ContainerModel;

import java.util.UUID;

// Todo: 目前只支持了本地Docker环境的测试
/**
 * Docker Sandbox Lifecycle Test
 * Tests sandbox creation, startup, status checking, stopping and cleanup functionality in Docker environment
 */
public class DockerLifecycleTest {

    private SandboxManager sandboxManager;
    private String testUserId;
    private String testSessionId;

    @BeforeEach
    void setUp() {
        // Generate test user ID and session ID
        testUserId = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
        testSessionId = "test-session-" + UUID.randomUUID().toString().substring(0, 8);

        // Initialize Docker sandbox manager
        try {
            BaseClientConfig config = new DockerClientConfig();
            sandboxManager = new SandboxManager(config);
            System.out.println("Docker SandboxManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Docker SandboxManager: " + e.getMessage());
            throw new RuntimeException("Failed to initialize test environment", e);
        }
    }

    @AfterEach
    void tearDown() {
        if (sandboxManager != null) {
            try {
                // Clean up all test-created sandboxes
                sandboxManager.cleanupAllSandboxes();
                System.out.println("All test sandboxes cleaned up successfully");
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }

    /**
     * Test base sandbox creation and startup
     */
    @Test
    void testCreateAndStartBaseSandbox() {
        System.out.println("Testing BASE sandbox creation and startup...");

        ContainerModel sandbox = sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);

        assertNotNull(sandbox, "Sandbox should be created successfully");
        assertNotNull(sandbox.getContainerId(), "Sandbox container ID should not be null");
        assertNotNull(sandbox.getContainerName(), "Sandbox container name should not be null");

        System.out.println("Created BASE sandbox: " + sandbox.getContainerId());

        // Check sandbox status
        String status = sandboxManager.getSandboxStatus(SandboxType.BASE, testUserId, testSessionId);
        assertNotNull(status, "Sandbox status should not be null");
        System.out.println("BASE sandbox status: " + status);
    }

    /**
     * Test browser sandbox creation and startup
     */
    @Test
    void testCreateAndStartBrowserSandbox() {
        System.out.println("Testing BROWSER sandbox creation and startup...");

        ContainerModel sandbox = sandboxManager.getSandbox(SandboxType.BROWSER, testUserId, testSessionId);

        assertNotNull(sandbox, "Browser sandbox should be created successfully");
        assertNotNull(sandbox.getContainerId(), "Browser sandbox container ID should not be null");
        assertNotNull(sandbox.getContainerName(), "Browser sandbox container name should not be null");

        System.out.println("Created BROWSER sandbox: " + sandbox.getContainerId());

        // Check sandbox status
        String status = sandboxManager.getSandboxStatus(SandboxType.BROWSER, testUserId, testSessionId);
        assertNotNull(status, "Browser sandbox status should not be null");
        System.out.println("BROWSER sandbox status: " + status);
    }

    /**
     * Test filesystem sandbox creation and startup
     */
    @Test
    void testCreateAndStartFilesystemSandbox() {
        System.out.println("Testing FILESYSTEM sandbox creation and startup...");

        ContainerModel sandbox = sandboxManager.getSandbox(SandboxType.FILESYSTEM, testUserId, testSessionId);

        assertNotNull(sandbox, "Filesystem sandbox should be created successfully");
        assertNotNull(sandbox.getContainerId(), "Filesystem sandbox container ID should not be null");
        assertNotNull(sandbox.getContainerName(), "Filesystem sandbox container name should not be null");

        System.out.println("Created FILESYSTEM sandbox: " + sandbox.getContainerId());

        // Check sandbox status
        String status = sandboxManager.getSandboxStatus(SandboxType.FILESYSTEM, testUserId, testSessionId);
        assertNotNull(status, "Filesystem sandbox status should not be null");
        System.out.println("FILESYSTEM sandbox status: " + status);
    }

    /**
     * Test training sandbox creation and startup
     */
    @Test
    void testCreateAndStartTrainingSandbox() {
        System.out.println("Testing TRAINING sandbox creation and startup...");

        ContainerModel sandbox = sandboxManager.getSandbox(SandboxType.TRAINING, testUserId, testSessionId);

        assertNotNull(sandbox, "Training sandbox should be created successfully");
        assertNotNull(sandbox.getContainerId(), "Training sandbox container ID should not be null");
        assertNotNull(sandbox.getContainerName(), "Training sandbox container name should not be null");

        System.out.println("Created TRAINING sandbox: " + sandbox.getContainerId());

        // Check sandbox status
        String status = sandboxManager.getSandboxStatus(SandboxType.TRAINING, testUserId, testSessionId);
        assertNotNull(status, "Training sandbox status should not be null");
        System.out.println("TRAINING sandbox status: " + status);
    }

    /**
     * Test multiple sandboxes running concurrently
     */
    @Test
    void testMultipleSandboxesConcurrently() {
        System.out.println("Testing multiple sandboxes running concurrently...");

        // Create multiple sandboxes of different types
        ContainerModel baseSandbox = sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        ContainerModel browserSandbox = sandboxManager.getSandbox(SandboxType.BROWSER, testUserId, testSessionId);
        ContainerModel filesystemSandbox = sandboxManager.getSandbox(SandboxType.FILESYSTEM, testUserId, testSessionId);
        ContainerModel trainingSandbox = sandboxManager.getSandbox(SandboxType.TRAINING, testUserId, testSessionId);

        // Verify all sandboxes are created successfully
        assertNotNull(baseSandbox, "Base sandbox should be created successfully");
        assertNotNull(browserSandbox, "Browser sandbox should be created successfully");
        assertNotNull(filesystemSandbox, "Filesystem sandbox should be created successfully");
        assertNotNull(trainingSandbox, "Training sandbox should be created successfully");

        // Verify all sandboxes have different container IDs
        assertNotEquals(baseSandbox.getContainerId(), browserSandbox.getContainerId(), "Different sandboxes should have different container IDs");
        assertNotEquals(baseSandbox.getContainerId(), filesystemSandbox.getContainerId(), "Different sandboxes should have different container IDs");
        assertNotEquals(browserSandbox.getContainerId(), filesystemSandbox.getContainerId(), "Different sandboxes should have different container IDs");
        assertNotEquals(trainingSandbox.getContainerId(), baseSandbox.getContainerId(), "Different sandboxes should have different container IDs");

        System.out.println("All sandboxes created successfully:");
        System.out.println("- BASE: " + baseSandbox.getContainerId());
        System.out.println("- BROWSER: " + browserSandbox.getContainerId());
        System.out.println("- FILESYSTEM: " + filesystemSandbox.getContainerId());
        System.out.println("- TRAINING: " + trainingSandbox.getContainerId());

        // Check all sandbox statuses
        String baseStatus = sandboxManager.getSandboxStatus(SandboxType.BASE, testUserId, testSessionId);
        String browserStatus = sandboxManager.getSandboxStatus(SandboxType.BROWSER, testUserId, testSessionId);
        String filesystemStatus = sandboxManager.getSandboxStatus(SandboxType.FILESYSTEM, testUserId, testSessionId);
        String trainingStatus = sandboxManager.getSandboxStatus(SandboxType.TRAINING, testUserId, testSessionId);
        assertNotNull(baseStatus, "Base sandbox status should not be null");
        assertNotNull(browserStatus, "Browser sandbox status should not be null");
        assertNotNull(filesystemStatus, "Filesystem sandbox status should not be null");
        assertNotNull(trainingStatus, "Training sandbox status should not be null");
    }

    /**
     * Test sandbox status checking
     */
    @Test
    void testSandboxStatusCheck() {
        System.out.println("Testing sandbox status check...");

        // Create sandbox
        ContainerModel sandbox = sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        assertNotNull(sandbox, "Sandbox should be created successfully");

        // Check sandbox status
        String status = sandboxManager.getSandboxStatus(SandboxType.BASE, testUserId, testSessionId);
        assertNotNull(status, "Sandbox status should not be null");
        assertNotEquals("not_found", status, "Sandbox status should not be 'not_found'");

        System.out.println("Sandbox status: " + status);
    }

    /**
     * Test sandbox stopping and cleanup
     */
    @Test
    void testStopAndRemoveSandbox() {
        System.out.println("Testing sandbox stop and removal...");

        // Create sandbox
        ContainerModel sandbox = sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        assertNotNull(sandbox, "Sandbox should be created successfully");

        String containerId = sandbox.getContainerId();
        System.out.println("Created sandbox: " + containerId);

        // Stop and remove sandbox
        sandboxManager.stopAndRemoveSandbox(SandboxType.BASE, testUserId, testSessionId);
        System.out.println("Sandbox stopped and removed");

        // Verify sandbox has been deleted
        String status = sandboxManager.getSandboxStatus(SandboxType.BASE, testUserId, testSessionId);
        assertEquals("not_found", status, "Sandbox status should be 'not_found' indicating it has been deleted");

        System.out.println("Sandbox successfully removed, status: " + status);
    }

    /**
     * Test cleanup of all sandboxes
     */
    @Test
    void testCleanupAllSandboxes() {
        System.out.println("Testing cleanup of all sandboxes...");

        // Create multiple sandboxes
        sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        sandboxManager.getSandbox(SandboxType.BROWSER, testUserId, testSessionId);
        sandboxManager.getSandbox(SandboxType.FILESYSTEM, testUserId, testSessionId);

        System.out.println("Created multiple sandboxes");

        // Clean up all sandboxes
        sandboxManager.cleanupAllSandboxes();
        System.out.println("All sandboxes cleaned up");

        // Verify all sandboxes have been deleted
        assertEquals("not_found", sandboxManager.getSandboxStatus(SandboxType.BASE, testUserId, testSessionId));
        assertEquals("not_found", sandboxManager.getSandboxStatus(SandboxType.BROWSER, testUserId, testSessionId));
        assertEquals("not_found", sandboxManager.getSandboxStatus(SandboxType.FILESYSTEM, testUserId, testSessionId));

        System.out.println("All sandboxes successfully cleaned up");
    }

    /**
     * Test getting all sandbox information
     */
    @Test
    void testGetAllSandboxes() {
        System.out.println("Testing get all sandboxes...");

        // Create some sandboxes
        sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        sandboxManager.getSandbox(SandboxType.BROWSER, testUserId, testSessionId);

        // Get all sandbox information
        var allSandboxes = sandboxManager.getAllSandboxes();
        assertNotNull(allSandboxes, "Sandbox mapping should not be null");
        assertTrue(allSandboxes.size() >= 2, "Should have at least 2 sandboxes");

        System.out.println("Total sandboxes: " + allSandboxes.size());
        allSandboxes.forEach((key, model) -> System.out.println("Sandbox: " + key + " -> " + model.getContainerId()));
    }

    /**
     * Test error case: duplicate creation of same sandbox
     */
    @Test
    void testDuplicateSandboxCreation() {
        System.out.println("Testing duplicate sandbox creation...");

        // Create first sandbox
        ContainerModel sandbox1 = sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        assertNotNull(sandbox1, "First sandbox should be created successfully");

        // Try to create sandbox of same type (should return existing sandbox)
        ContainerModel sandbox2 = sandboxManager.getSandbox(SandboxType.BASE, testUserId, testSessionId);
        assertNotNull(sandbox2, "Should return existing sandbox");
        assertEquals(sandbox1.getContainerId(), sandbox2.getContainerId(), "Duplicate creation should return the same sandbox instance");

        System.out.println("Duplicate sandbox creation handled correctly");
    }
}
