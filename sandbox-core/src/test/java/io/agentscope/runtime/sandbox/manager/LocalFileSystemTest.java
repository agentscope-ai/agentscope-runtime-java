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

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemStarter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local File System Storage Functionality Test
 * <p>
 * Test Features:
 * 1. Configure local file system storage
 * 2. Copy data from local storage directory to container mount directory
 * 3. Container operations
 * 4. Copy data back to local storage directory when destroying container
 * <p>
 * Environment Variable Configuration:
 * - LOCAL_STORAGE_PATH: Local storage root directory path (optional, defaults to ./test_storage)
 */
@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
public class LocalFileSystemTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private String localStoragePath;
    private SandboxService sandboxService;

    @BeforeEach
    void setUp() {
        // Read local storage path from environment variables, use default if not set
        localStoragePath = System.getenv("LOCAL_STORAGE_PATH");
        
        if (localStoragePath == null || localStoragePath.isEmpty()) {
            // Use default test storage path
            localStoragePath = "./test_storage";
            System.out.println("Environment variable LOCAL_STORAGE_PATH is not configured, using default path: " + localStoragePath);
        }

        // Ensure local storage path exists
        File storageDir = new File(localStoragePath);
        if (!storageDir.exists()) {
            assertTrue(storageDir.mkdirs(), "Should be able to create local storage directory");
            System.out.println("Created local storage directory: " + localStoragePath);
        }

        System.out.println("Local File System Configuration:");
        System.out.println("  - Storage Path: " + localStoragePath);
    }

    @AfterEach
    void tearDown() {
        if (sandboxService != null) {
            try {
                sandboxService.close();
                System.out.println("SandboxService closed successfully");
            } catch (Exception e) {
                System.err.println("Error closing SandboxService: " + e.getMessage());
            }
        }
    }

    /**
     * Test creating and managing containers with local file system storage
     */
    @Test
    void testLocalFileSystemIntegration() {
        System.out.println("\n--- Testing Local File System Storage Integration ---");

        try {
            // 1. Prepare test data - create test folder and files under local storage path
            String storagePath = "test_session_folder";
            File testFolder = new File(localStoragePath, storagePath);
            if (!testFolder.exists()) {
                assertTrue(testFolder.mkdirs(), "Should be able to create test folder");
            }
            
            // Create some initial test files
            System.out.println("\n--- Preparing test data ---");
            createInitialTestFiles(testFolder.getAbsolutePath());

            // 2. Configure local file system storage
            LocalFileSystemStarter localFileSystemStarter = LocalFileSystemStarter.builder()
                    .storageFolderPath(localStoragePath)
                    .build();

            System.out.println("  - Local Storage Path: " + localStoragePath);

            // 3. Create ManagerConfig
            ManagerConfig config = new ManagerConfig.Builder()
                    .build();

            // 4. Create SandboxService
            sandboxService = new SandboxService(config);
            sandboxService.start();
            System.out.println("\nSandboxService with local file system storage created successfully");

            // 5. Create container (will copy data from local storage)
            System.out.println("\n--- Creating container ---");

            // Use the just created test folder
            System.out.println("Copying from local path: " + storagePath);

            Sandbox sandbox = new BaseSandbox(sandboxService, "test-user", "test-session", localFileSystemStarter);

            // Verify container created successfully
            assertNotNull(sandbox, "Container creation should succeed");
            assertNotNull(sandbox.getSandboxId(), "Container ID should not be null");

            // 6. Verify copied files
            System.out.println("\n--- Verifying files copied from local storage ---");
            File mountDirectory = new File(localFileSystemStarter.getMountDir());
            assertTrue(mountDirectory.exists(), "Mount directory should exist");

            System.out.println("Files in mount directory:");
            listFilesRecursively(mountDirectory, "  ");

            // 7. Create new files in container (for testing upload functionality)
            System.out.println("\n--- Creating new files in container ---");
            createTestFilesInContainer(localFileSystemStarter.getMountDir());

            // 8. Display file list again
            System.out.println("\nUpdated file list:");
            listFilesRecursively(mountDirectory, "  ");

            // 9. Destroy container and upload data
            System.out.println("\n--- Destroying container and copying data back to local storage ---");
            boolean released = sandboxService.stopAndRemoveSandbox(sandbox.getSandboxId());
            assertTrue(released, "Container should be released successfully");
            System.out.println("Container destroyed, data copied back to local storage: " + localFileSystemStarter.getStorageFolderPath());

            // 10. Verify data has been copied back to local storage
            System.out.println("\n--- Verifying files in local storage ---");
            File storageFolder = new File(localStoragePath, storagePath);
            assertTrue(storageFolder.exists(), "Local storage folder should exist");
            System.out.println("Files in local storage folder:");
            listFilesRecursively(storageFolder, "  ");

        } catch (Exception e) {
            fail("Test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test local file system configuration validation
     */
    @Test
    void testLocalFileSystemConfigValidation() {
        System.out.println("\n--- Testing Local File System Config Validation ---");

        // Create local file system configuration
        LocalFileSystemStarter localFileSystemStarter = LocalFileSystemStarter.builder()
                .storageFolderPath(localStoragePath)
                .mountDir("custom_mount_dir")
                .build();

        // Validate configuration
        assertEquals(localStoragePath, localFileSystemStarter.getStorageFolderPath(), "Storage path should match");
        assertEquals("custom_mount_dir", localFileSystemStarter.getMountDir(), "Mount directory should match");

        System.out.println("Local file system configuration validated successfully");
        System.out.println("  - Storage Path: " + localFileSystemStarter.getStorageFolderPath());
        System.out.println("  - Mount Dir: " + localFileSystemStarter.getMountDir());
    }

    /**
     * Create initial test files
     */
    private void createInitialTestFiles(String folderPath) {
        try {
            // Create an initial text file
            File initialFile = new File(folderPath, "initial_file.txt");
            try (FileWriter writer = new FileWriter(initialFile)) {
                writer.write("This file was initially stored in the local file system\n");
                writer.write("Created at: " + new java.util.Date() + "\n");
                writer.write("Used for testing copy from local storage to container\n");
            }
            System.out.println("  Created initial file: " + initialFile.getName());

            // Create a configuration file
            File configFile = new File(folderPath, "config.json");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("{\n");
                writer.write("  \"name\": \"Local Test Configuration\",\n");
                writer.write("  \"timestamp\": \"" + System.currentTimeMillis() + "\",\n");
                writer.write("  \"source\": \"Local File System\"\n");
                writer.write("}\n");
            }
            System.out.println("  Created config file: " + configFile.getName());

        } catch (IOException e) {
            fail("Failed to create initial test files: " + e.getMessage(), e);
        }
    }

    /**
     * Create test files in container
     */
    private void createTestFilesInContainer(String mountDir) {
        try {
            // Create a new text file
            File newFile1 = new File(mountDir, "container_created_file.txt");
            try (FileWriter writer = new FileWriter(newFile1)) {
                writer.write("This file was created in the container\n");
                writer.write("Created at: " + new java.util.Date() + "\n");
                writer.write("Used for testing copy back to local storage functionality\n");
            }
            System.out.println("  Created file: " + newFile1.getName());

            // Create a JSON file
            File newFile2 = new File(mountDir, "test_data.json");
            try (FileWriter writer = new FileWriter(newFile2)) {
                writer.write("{\n");
                writer.write("  \"message\": \"Container test file\",\n");
                writer.write("  \"timestamp\": \"" + System.currentTimeMillis() + "\",\n");
                writer.write("  \"source\": \"Java Container Test\"\n");
                writer.write("}\n");
            }
            System.out.println("  Created file: " + newFile2.getName());

            // Create a subdirectory and file
            File subDir = new File(mountDir, "test_dir");
            if (!subDir.exists()) {
                assertTrue(subDir.mkdir(), "Should be able to create subdirectory");
            }

            File subFile = new File(subDir, "nested_file.txt");
            try (FileWriter writer = new FileWriter(subFile)) {
                writer.write("This is a file in the subdirectory\n");
            }
            System.out.println("  Created nested file: test_dir/" + subFile.getName());

        } catch (IOException e) {
            fail("Failed to create test files: " + e.getMessage(), e);
        }
    }

    /**
     * Recursively list all files in directory
     */
    private void listFilesRecursively(File dir, String indent) {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println(indent + "(empty directory)");
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println(indent + "📁 " + file.getName() + "/");
                listFilesRecursively(file, indent + "  ");
            } else {
                long sizeKB = file.length() / 1024;
                String sizeStr = sizeKB > 0 ? sizeKB + " KB" : file.length() + " bytes";
                System.out.println(indent + "📄 " + file.getName() + " (" + sizeStr + ")");
            }
        }
    }
}
