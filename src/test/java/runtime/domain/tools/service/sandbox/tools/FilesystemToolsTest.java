package runtime.domain.tools.service.sandbox.tools;

import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilesystemToolsTest{

    private SandboxManager sandboxManager;
    private FilesystemSandbox sandbox;

    @BeforeEach
    void setUp() {
        // Initialize sandbox manager
        try {
            BaseClientConfig clientConfig = DockerClientConfig.builder().build();
            ManagerConfig config = new ManagerConfig.Builder()
                    .containerDeployment(clientConfig)
                    .build();
            sandboxManager = new SandboxManager(config);
            sandbox = new FilesystemSandbox(sandboxManager, "test-user", "test-session");
            System.out.println("SandboxManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize SandboxManager: " + e.getMessage());
            throw new RuntimeException("Failed to initialize test environment", e);
        }
    }

    @AfterEach
    void tearDown() {
        if (sandbox != null) {
            try {
                sandbox.close();
            } catch (Exception e) {
                System.err.println("Error closing sandbox: " + e.getMessage());
            }
        }
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

    @Test
    void testReadWriteAndEditFile() {
        // Test file write
        String write = sandbox.writeFile("/workspace/test.txt", "hello");
        System.out.println("write: " + write);
        assertNotNull(write);

        // Test file read
        String read = sandbox.readFile("/workspace/test.txt");
        System.out.println("read: " + read);
        assertNotNull(read);
        assertTrue(read.contains("hello"), "File content should contain 'hello'");

        // Test file edit
        Object[] edits = new Object[] {
            new java.util.HashMap<String, String>() {{
                put("oldText", "hello");
                put("newText", "world");
            }}
        };
        String edited = sandbox.editFile("/workspace/test.txt", edits);
        System.out.println("edited: " + edited);
        assertNotNull(edited);

        // Verify edit
        read = sandbox.readFile("/workspace/test.txt");
        System.out.println("read after edit: " + read);
        assertNotNull(read);
        assertTrue(read.contains("world"), "File content should contain 'world' after edit");
    }

    @Test
    void testReadMultipleFiles() {
        // Create test files
        sandbox.writeFile("/workspace/test1.txt", "content1");
        sandbox.writeFile("/workspace/test2.txt", "content2");

        // Test reading multiple files
        java.util.List<String> paths = java.util.Arrays.asList("/workspace/test1.txt", "/workspace/test2.txt");
        String result = sandbox.readMultipleFiles(paths);
        System.out.println("read multiple files: " + result);
        assertNotNull(result);
        assertTrue(result.contains("content1") || result.contains("test1.txt"), "Result should contain content from first file");
        assertTrue(result.contains("content2") || result.contains("test2.txt"), "Result should contain content from second file");
    }

    @Test
    void testDirectoryOps() {
        // Test directory creation
        String created = sandbox.createDirectory("/workspace/dirA");
        System.out.println("created: " + created);
        assertNotNull(created);

        // Test directory listing
        String list = sandbox.listDirectory("/workspace");
        System.out.println("list: " + list);
        assertNotNull(list);
        assertTrue(list.contains("dirA"), "Directory listing should contain 'dirA'");

        // Test directory tree
        String tree = sandbox.directoryTree("/workspace");
        System.out.println("tree: " + tree);
        assertNotNull(tree);
        assertTrue(tree.contains("dirA"), "Directory tree should contain 'dirA'");
    }

    @Test
    void testMoveSearchInfoAllowed() {
        // Create a test file
        String write = sandbox.writeFile("/workspace/test.txt", "hello");
        System.out.println("write: " + write);
        assertNotNull(write);

        // Test file move
        String moved = sandbox.moveFile("/workspace/test.txt", "/workspace/test-moved.txt");
        System.out.println("moved: " + moved);
        assertNotNull(moved);

        // Test file search
        String search = sandbox.searchFiles("/workspace", "test-moved.txt", null);
        System.out.println("search: " + search);
        assertNotNull(search);
        assertTrue(search.contains("test-moved.txt"), "Search result should contain the moved file");

        // Test get file info
        String info = sandbox.getFileInfo("/workspace/test-moved.txt");
        System.out.println("info: " + info);
        assertNotNull(info);

        // Test list allowed directories
        String allowed = sandbox.listAllowedDirectories();
        System.out.println("allowed: " + allowed);
        assertNotNull(allowed);
    }
}


