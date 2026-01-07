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
import io.agentscope.runtime.sandbox.manager.fs.oss.OssStarter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OSS Storage Functionality Test
 * <p>
 * Test Features:
 * 1. Configure OSS storage
 * 2. Download data from OSS to container mount directory
 * 3. Container operations
 * 4. Upload data back to OSS when destroying container
 * <p>
 * Environment Variable Configuration:
 * - OSS_ENDPOINT: OSS access endpoint
 * - OSS_ACCESS_KEY_ID: OSS access key ID
 * - OSS_ACCESS_KEY_SECRET: OSS access key secret
 * - OSS_BUCKET_NAME: OSS bucket name
 */
@EnabledIfEnvironmentVariable(named = "OSS_ENDPOINT", matches = ".+")
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
public class OssStorageTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private String ossEndpoint;
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossBucketName;
    private SandboxService sandboxService;

    @BeforeEach
    void setUp() {
        // Read OSS configuration from environment variables
        ossEndpoint = System.getenv("OSS_ENDPOINT");
        ossAccessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        ossAccessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        ossBucketName = System.getenv("OSS_BUCKET_NAME");

        // Verify environment variables are configured
        Assertions.assertNotNull(ossEndpoint, "Environment variable OSS_ENDPOINT is not configured");
        Assertions.assertNotNull(ossAccessKeyId, "Environment variable OSS_ACCESS_KEY_ID is not configured");
        Assertions.assertNotNull(ossAccessKeySecret, "Environment variable OSS_ACCESS_KEY_SECRET is not configured");
        Assertions.assertNotNull(ossBucketName, "Environment variable OSS_BUCKET_NAME is not configured");

        Assertions.assertFalse(ossEndpoint.isEmpty(), "OSS_ENDPOINT cannot be empty");
        Assertions.assertFalse(ossAccessKeyId.isEmpty(), "OSS_ACCESS_KEY_ID cannot be empty");
        Assertions.assertFalse(ossAccessKeySecret.isEmpty(), "OSS_ACCESS_KEY_SECRET cannot be empty");
        Assertions.assertFalse(ossBucketName.isEmpty(), "OSS_BUCKET_NAME cannot be empty");

        System.out.println("OSS Configuration:");
        System.out.println("  - Endpoint: " + ossEndpoint);
        System.out.println("  - Bucket: " + ossBucketName);
        System.out.println("  - Access Key ID: " + ossAccessKeyId.substring(0, Math.min(4, ossAccessKeyId.length())) + "****");
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
     * Test creating and managing containers with OSS storage
     */
    @Test
    void testOssStorageIntegration() {
        System.out.println("\n--- Testing OSS Storage Integration ---");

        try {
            // 1. Configure OSS storage
            OssStarter ossConfig = OssStarter.builder()
                    .ossEndpoint(ossEndpoint)
                    .ossAccessKeyId(ossAccessKeyId)
                    .ossAccessKeySecret(ossAccessKeySecret)
                    .ossBucketName(ossBucketName)
                    .storageFolderPath("folder")
                    .build();

            System.out.println("  - Storage Folder: folder");

            // 2. Create ManagerConfig
            ManagerConfig config = new ManagerConfig.Builder()
                    .baseUrl("http://localhost:10001")
                    .build();

            // 3. Create SandboxService
            sandboxService = new SandboxService(config);
            sandboxService.start();

            // 4. Create container (will download data from OSS)
            System.out.println("\n--- Creating container ---");

            String storagePath = "folder";  // Fixed: download all contents under folder/
            System.out.println("Downloading from OSS path: " + storagePath);

            Sandbox sandbox = new BaseSandbox(sandboxService, "oss_test_user", "oss_test_session_001", ossConfig);

            // Verify container created successfully
            Assertions.assertNotNull(sandbox, "Container creation should succeed");
            Assertions.assertNotNull(sandbox.getSandboxId(), "Container ID should not be null");

            // 5. Verify downloaded files
            System.out.println("\n--- Verifying downloaded files from OSS ---");
            File mountDirectory = new File(ossConfig.getMountDir());
            Assertions.assertTrue(mountDirectory.exists(), "Mount directory should exist");

            System.out.println("Files in mount directory:");
            listFilesRecursively(mountDirectory, "  ");

            // 6. Create new files in container (for testing upload functionality)
            System.out.println("\n--- Creating new files in container ---");
            createTestFilesInContainer(ossConfig.getMountDir());

            // 7. Display file list again
            System.out.println("\nUpdated file list:");
            listFilesRecursively(mountDirectory, "  ");

            // 8. Destroy container and upload data
            System.out.println("\n--- Destroying container and uploading data to OSS ---");
            boolean released = sandboxService.stopAndRemoveSandbox(sandbox.getSandboxId());
            Assertions.assertTrue(released, "Container should be released successfully");
            System.out.println("Container destroyed, data uploaded to OSS: " + ossConfig.getStorageFolderPath());

        } catch (Exception e) {
            Assertions.fail("Test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test OSS configuration validation
     */
    @Test
    void testOssConfigValidation() {
        System.out.println("\n--- Testing OSS Config Validation ---");

        // Create OSS configuration
        OssStarter ossConfig = OssStarter.builder()
                .ossEndpoint(ossEndpoint)
                .ossAccessKeyId(ossAccessKeyId)
                .ossAccessKeySecret(ossAccessKeySecret)
                .ossBucketName(ossBucketName)
                .storageFolderPath("test-folder")
                .build();

        // Validate configuration
        assertEquals(ossEndpoint, ossConfig.getOssEndpoint(), "Endpoint should match");
        assertEquals(ossAccessKeyId, ossConfig.getOssAccessKeyId(), "Access Key ID should match");
        assertEquals(ossAccessKeySecret, ossConfig.getOssAccessKeySecret(), "Access Key Secret should match");
        assertEquals(ossBucketName, ossConfig.getOssBucketName(), "Bucket Name should match");
        assertEquals("test-folder", ossConfig.getStorageFolderPath(), "Storage Folder Path should match");

        System.out.println("OSS configuration validated successfully");
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
                writer.write("Used for testing upload to OSS functionality\n");
            }
            System.out.println("  Created file: " + newFile1.getName());

            // Create a JSON file
            File newFile2 = new File(mountDir, "test_data.json");
            try (FileWriter writer = new FileWriter(newFile2)) {
                writer.write("{\n");
                writer.write("  \"message\": \"Test file\",\n");
                writer.write("  \"timestamp\": \"" + System.currentTimeMillis() + "\",\n");
                writer.write("  \"source\": \"Java Container Test\"\n");
                writer.write("}\n");
            }
            System.out.println("  Created file: " + newFile2.getName());

            // Create a subdirectory and file
            File subDir = new File(mountDir, "test_dir");
            if (!subDir.exists()) {
                Assertions.assertTrue(subDir.mkdir(), "Should be able to create subdirectory");
            }

            File subFile = new File(subDir, "nested_file.txt");
            try (FileWriter writer = new FileWriter(subFile)) {
                writer.write("This is a file in the subdirectory\n");
            }
            System.out.println("  Created nested file: test_dir/" + subFile.getName());

        } catch (IOException e) {
            Assertions.fail("Failed to create test files: " + e.getMessage(), e);
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
