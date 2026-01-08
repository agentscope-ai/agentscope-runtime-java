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

package io.agentscope.runtime.sandbox.manager;

import io.agentscope.runtime.sandbox.box.*;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Todo: Currently only supports testing for local Docker environment

/**
 * Docker Sandbox Lifecycle Test
 * Tests sandbox creation, startup, status checking, stopping and cleanup functionality in Docker environment
 */
@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
public class DockerLifecycleTest {

    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private SandboxService sandboxService;
    private String testUserId;
    private String testSessionId;

    @BeforeEach
    void setUp() {
        // Generate test user ID and session ID
        testUserId = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
        testSessionId = "test-session-" + UUID.randomUUID().toString().substring(0, 8);

        // Initialize Docker sandbox manager
        try {
            BaseClientStarter clientConfig = DockerClientStarter.builder().build();
            ManagerConfig config = new ManagerConfig.Builder()
                    .clientStarter(clientConfig)
                    .build();
            sandboxService = new SandboxService(config);
            sandboxService.start();
            System.out.println("Docker SandboxService initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Docker SandboxService: " + e.getMessage());
            throw new RuntimeException("Failed to initialize test environment", e);
        }
    }

    @AfterEach
    void tearDown() {
        if (sandboxService != null) {
            try {
                // Clean up all test-created sandboxes
                sandboxService.cleanupAllSandboxes();
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

        Sandbox sandbox = new BaseSandbox(sandboxService, testUserId, testSessionId);

        assertNotNull(sandbox, "Sandbox should be created successfully");
        assertNotNull(sandbox.getSandboxId(), "Sandbox container ID should not be null");

        System.out.println("Created BASE sandbox: " + sandbox.getSandboxId());

        // Check sandbox status
        String status = sandboxService.getSandboxStatus(testUserId, testSessionId, "base");
        assertNotNull(status, "Sandbox status should not be null");
        System.out.println("BASE sandbox status: " + status);

        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test browser sandbox creation and startup
     */
    @Test
    void testCreateAndStartBrowserSandbox() {
        System.out.println("Testing BROWSER sandbox creation and startup...");

        Sandbox sandbox = new BrowserSandbox(sandboxService, testUserId, testSessionId);

        assertNotNull(sandbox, "Browser sandbox should be created successfully");
        assertNotNull(sandbox.getSandboxId(), "Browser sandbox container ID should not be null");

        System.out.println("Created BROWSER sandbox: " + sandbox.getSandboxId());

        // Check sandbox status
        String status = sandboxService.getSandboxStatus(testUserId, testSessionId, "browser");
        assertNotNull(status, "Browser sandbox status should not be null");
        System.out.println("BROWSER sandbox status: " + status);
        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test filesystem sandbox creation and startup
     */
    @Test
    void testCreateAndStartFilesystemSandbox() {
        System.out.println("Testing FILESYSTEM sandbox creation and startup...");

        Sandbox sandbox = new FilesystemSandbox(sandboxService, testUserId, testSessionId);

        assertNotNull(sandbox, "Filesystem sandbox should be created successfully");
        assertNotNull(sandbox.getSandboxId(), "Filesystem sandbox container ID should not be null");

        System.out.println("Created FILESYSTEM sandbox: " + sandbox.getSandboxId());

        // Check sandbox status
        String status = sandboxService.getSandboxStatus(testUserId, testSessionId, "filesystem");
        assertNotNull(status, "Filesystem sandbox status should not be null");
        System.out.println("FILESYSTEM sandbox status: " + status);
        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test training sandbox creation and startup
     */
    @Test
    void testCreateAndStartTrainingSandbox() {
        System.out.println("Testing TRAINING sandbox creation and startup...");

        Sandbox sandbox = new TrainingSandbox(sandboxService, testUserId, testSessionId, "training");

        assertNotNull(sandbox, "Training sandbox should be created successfully");

        System.out.println("Created TRAINING sandbox: " + sandbox.getSandboxId());

        // Check sandbox status
        String status = sandboxService.getSandboxStatus(testUserId, testSessionId, "training");
        assertNotNull(status, "Training sandbox status should not be null");
        System.out.println("TRAINING sandbox status: " + status);
        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test multiple sandboxes running concurrently
     */
    @Test
    void testMultipleSandboxesConcurrently() {
        System.out.println("Testing multiple sandboxes running concurrently...");

        Sandbox baseSandbox = new BaseSandbox(sandboxService, testUserId, testSessionId);
        Sandbox browserSandbox = new BrowserSandbox(sandboxService, testUserId, testSessionId);
        Sandbox filesystemSandbox = new FilesystemSandbox(sandboxService, testUserId, testSessionId);
        Sandbox trainingSandbox = new TrainingSandbox(sandboxService, testUserId, testSessionId, "training");

        // Verify all sandboxes are created successfully
        assertNotNull(baseSandbox, "Base sandbox should be created successfully");
        assertNotNull(browserSandbox, "Browser sandbox should be created successfully");
        assertNotNull(filesystemSandbox, "Filesystem sandbox should be created successfully");
        assertNotNull(trainingSandbox, "Training sandbox should be created successfully");

        // Verify all sandboxes have different container IDs
        assertNotEquals(baseSandbox.getSandboxId(), browserSandbox.getSandboxId(), "Different sandboxes should have different container IDs");
        assertNotEquals(baseSandbox.getSandboxId(), filesystemSandbox.getSandboxId(), "Different sandboxes should have different container IDs");
        assertNotEquals(browserSandbox.getSandboxId(), filesystemSandbox.getSandboxId(), "Different sandboxes should have different container IDs");
        assertNotEquals(trainingSandbox.getSandboxId(), baseSandbox.getSandboxId(), "Different sandboxes should have different container IDs");

        System.out.println("All sandboxes created successfully:");
        System.out.println("- BASE: " + baseSandbox.getSandboxId());
        System.out.println("- BROWSER: " + browserSandbox.getSandboxId());
        System.out.println("- FILESYSTEM: " + filesystemSandbox.getSandboxId());
        System.out.println("- TRAINING: " + trainingSandbox.getSandboxId());

        // Check all sandbox statuses
        String baseStatus = sandboxService.getSandboxStatus(testUserId, testSessionId, "base");
        String browserStatus = sandboxService.getSandboxStatus(testUserId, testSessionId, "browser");
        String filesystemStatus = sandboxService.getSandboxStatus(testUserId, testSessionId, "filesystem");
        String trainingStatus = sandboxService.getSandboxStatus(testUserId, testSessionId, "training");
        assertNotNull(baseStatus, "Base sandbox status should not be null");
        assertNotNull(browserStatus, "Browser sandbox status should not be null");
        assertNotNull(filesystemStatus, "Filesystem sandbox status should not be null");
        assertNotNull(trainingStatus, "Training sandbox status should not be null");

        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test sandbox status checking
     */
    @Test
    void testSandboxStatusCheck() {
        System.out.println("Testing sandbox status check...");

        // Create sandbox
        Sandbox sandbox = new BaseSandbox(sandboxService, testUserId, testSessionId);
        assertNotNull(sandbox, "Sandbox should be created successfully");

        // Check sandbox status
        String status = sandboxService.getSandboxStatus(testUserId, testSessionId, "base");
        assertNotNull(status, "Sandbox status should not be null");
        assertNotEquals("not_found", status, "Sandbox status should not be 'not_found'");

        System.out.println("Sandbox status: " + status);

        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test sandbox stopping and cleanup
     */
    @Test
    void testStopAndRemoveSandbox() {
        System.out.println("Testing sandbox stop and removal...");

        // Create sandbox
        Sandbox sandbox = new BaseSandbox(sandboxService, testUserId, testSessionId);
        assertNotNull(sandbox, "Sandbox should be created successfully");

        String containerId = sandbox.getSandboxId();
        System.out.println("Created sandbox: " + containerId);

        // Stop and remove sandbox
        sandboxService.stopAndRemoveSandbox(testUserId, testSessionId, "base");
        System.out.println("Sandbox stopped and removed");

        // Verify sandbox has been deleted
        String status = sandboxService.getSandboxStatus(testUserId, testSessionId, "base");
        assertEquals("not_found", status, "Sandbox status should be 'not_found' indicating it has been deleted");

        System.out.println("Sandbox successfully removed, status: " + status);

        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test cleanup of all sandboxes
     */
    @Test
    void testCleanupAllSandboxes() {
        System.out.println("Testing cleanup of all sandboxes...");

        // Create multiple sandboxes
        Sandbox baseSandbox = new BaseSandbox(sandboxService, testUserId, testSessionId);
        Sandbox browserSandbox = new BrowserSandbox(sandboxService, testUserId, testSessionId);
        Sandbox filesystemSandbox = new FilesystemSandbox(sandboxService, testUserId, testSessionId);

        System.out.println("Created multiple sandboxes");

        // Clean up all sandboxes
        sandboxService.cleanupAllSandboxes();
        System.out.println("All sandboxes cleaned up");

        // Verify all sandboxes have been deleted
        assertEquals("not_found", sandboxService.getSandboxStatus(testUserId, testSessionId, "base"));
        assertEquals("not_found", sandboxService.getSandboxStatus(testUserId, testSessionId, "browser"));
        assertEquals("not_found", sandboxService.getSandboxStatus(testUserId, testSessionId, "filesystem"));

        System.out.println("All sandboxes successfully cleaned up");

        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test getting all sandbox information
     */
    @Test
    void testGetAllSandboxes() {
        System.out.println("Testing get all sandboxes...");

        // Create multiple sandboxes
        Sandbox baseSandbox = new BaseSandbox(sandboxService, testUserId, testSessionId);
        Sandbox browserSandbox = new BrowserSandbox(sandboxService, testUserId, testSessionId);

        // Get all sandbox information
        var allSandboxes = sandboxService.getAllSandboxes();
        assertNotNull(allSandboxes, "Sandbox mapping should not be null");
        assertTrue(allSandboxes.size() >= 2, "Should have at least 2 sandboxes");

        System.out.println("Total sandboxes: " + allSandboxes.size());
        allSandboxes.forEach((key, model) -> System.out.println("Sandbox: " + key + " -> " + model.getContainerId()));

        sandboxService.cleanupAllSandboxes();
    }

    /**
     * Test error case: duplicate creation of same sandbox
     */
    @Test
    void testDuplicateSandboxCreation() {
        System.out.println("Testing duplicate sandbox creation...");

        // Create first sandbox
        Sandbox sandbox1 = new BaseSandbox(sandboxService, testUserId, testSessionId);
        assertNotNull(sandbox1, "First sandbox should be created successfully");

        // Try to create sandbox of same type (should return existing sandbox)
        Sandbox sandbox2 = new BaseSandbox(sandboxService, testUserId, testSessionId);
        assertNotNull(sandbox2, "Should return existing sandbox");
        assertEquals(sandbox1.getSandboxId(), sandbox2.getSandboxId(), "Duplicate creation should return the same sandbox instance");

        System.out.println("Duplicate sandbox creation handled correctly");

        sandboxService.cleanupAllSandboxes();
    }
}
