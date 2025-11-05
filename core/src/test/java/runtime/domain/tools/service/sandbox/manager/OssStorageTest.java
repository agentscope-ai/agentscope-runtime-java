package runtime.domain.tools.service.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.fs.OssConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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
public class OssStorageTest {

    private String ossEndpoint;
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossBucketName;
    private SandboxManager manager;

    @BeforeEach
    void setUp() {
        // Read OSS configuration from environment variables
        ossEndpoint = System.getenv("OSS_ENDPOINT");
        ossAccessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        ossAccessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        ossBucketName = System.getenv("OSS_BUCKET_NAME");

        // Verify environment variables are configured
        assertNotNull(ossEndpoint, "Environment variable OSS_ENDPOINT is not configured");
        assertNotNull(ossAccessKeyId, "Environment variable OSS_ACCESS_KEY_ID is not configured");
        assertNotNull(ossAccessKeySecret, "Environment variable OSS_ACCESS_KEY_SECRET is not configured");
        assertNotNull(ossBucketName, "Environment variable OSS_BUCKET_NAME is not configured");

        assertFalse(ossEndpoint.isEmpty(), "OSS_ENDPOINT cannot be empty");
        assertFalse(ossAccessKeyId.isEmpty(), "OSS_ACCESS_KEY_ID cannot be empty");
        assertFalse(ossAccessKeySecret.isEmpty(), "OSS_ACCESS_KEY_SECRET cannot be empty");
        assertFalse(ossBucketName.isEmpty(), "OSS_BUCKET_NAME cannot be empty");

        System.out.println("OSS Configuration:");
        System.out.println("  - Endpoint: " + ossEndpoint);
        System.out.println("  - Bucket: " + ossBucketName);
        System.out.println("  - Access Key ID: " + ossAccessKeyId.substring(0, Math.min(4, ossAccessKeyId.length())) + "****");
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            try {
                manager.close();
                System.out.println("SandboxManager closed successfully");
            } catch (Exception e) {
                System.err.println("Error closing SandboxManager: " + e.getMessage());
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
            OssConfig ossConfig = OssConfig.builder()
                    .ossEndpoint(ossEndpoint)
                    .ossAccessKeyId(ossAccessKeyId)
                    .ossAccessKeySecret(ossAccessKeySecret)
                    .ossBucketName(ossBucketName)
                    .storageFolderPath("folder")
                    .build();

            System.out.println("  - Storage Folder: folder");

            // 2. Create ManagerConfig
            ManagerConfig config = new ManagerConfig.Builder()
                    .poolSize(0)
                    .fileSystemConfig(ossConfig)
                    .build();

            // 3. Create SandboxManager
            manager = new SandboxManager(config);
            System.out.println("\nSandboxManager with OSS storage created successfully");

            // 4. Create container (will download data from OSS)
            System.out.println("\n--- Creating container ---");

            String storagePath = "folder";  // Fixed: download all contents under folder/
            System.out.println("Downloading from OSS path: " + storagePath);

            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE,
                    null,
                    storagePath,
                    null,  // environment
                    "oss_test_user",
                    "oss_test_session_001"
            );

            // Verify container created successfully
            assertNotNull(container, "Container creation should succeed");
            assertNotNull(container.getContainerName(), "Container name should not be null");
            assertNotNull(container.getMountDir(), "Mount directory should not be null");
            assertEquals(storagePath, container.getStoragePath(), "Storage path should match");

            System.out.println("\nContainer created with OSS storage:");
            System.out.println("  - Container: " + container.getContainerName());
            System.out.println("  - Mount Dir: " + container.getMountDir());
            System.out.println("  - Storage Path: " + container.getStoragePath());

            // 5. Verify downloaded files
            System.out.println("\n--- Verifying downloaded files from OSS ---");
            File mountDirectory = new File(container.getMountDir());
            assertTrue(mountDirectory.exists(), "Mount directory should exist");

            System.out.println("Files in mount directory:");
            listFilesRecursively(mountDirectory, "  ");

            // 6. Create new files in container (for testing upload functionality)
            System.out.println("\n--- Creating new files in container ---");
            createTestFilesInContainer(container.getMountDir());

            // 7. Display file list again
            System.out.println("\nUpdated file list:");
            listFilesRecursively(mountDirectory, "  ");

            // 8. Destroy container and upload data
            System.out.println("\n--- Destroying container and uploading data to OSS ---");
            boolean released = manager.release(container.getContainerName());
            assertTrue(released, "Container should be released successfully");
            System.out.println("Container destroyed, data uploaded to OSS: " + container.getStoragePath());

        } catch (Exception e) {
            fail("Test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test OSS configuration validation
     */
    @Test
    void testOssConfigValidation() {
        System.out.println("\n--- Testing OSS Config Validation ---");

        // Create OSS configuration
        OssConfig ossConfig = OssConfig.builder()
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
