package runtime.domain.tools.service.sandbox.tools;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

public class FilesystemToolsTest{

    private SandboxManager sandboxManager;

    @BeforeEach
    void setUp() {
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

    @Test
    void testReadWriteAndEditFile() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String write = tools.fs_write_file("/workspace/test.txt", "hello", "", "");
        System.out.println("write: "+write);
        assertNotNull(write);

        String read = tools.fs_read_file("/workspace/test.txt", "", "");
        System.out.println("read: "+read);
        assertNotNull(read);

        Object[] edits = new Object[] { new java.util.HashMap<String, Object>() {{ put("oldText", "hello"); put("newText", "world"); }} };
        String edited = tools.fs_edit_file("/workspace/test.txt", edits, "", "");
        System.out.println("edited: "+edited);
        assertNotNull(edited);
    }

    @Test
    void testReadMultipleFiles() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        tools.fs_write_file("/workspace/test1.txt", "content1", "", "");
        tools.fs_write_file("/workspace/test2.txt", "content2", "", "");

        String[] paths = {"/workspace/test1.txt", "/workspace/test2.txt"};
        String result = tools.fs_read_multiple_files(paths, "", "");
        System.out.println("read multiple files: " + result);
        assertNotNull(result);
    }

    @Test
    void testDirectoryOps() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String created = tools.fs_create_directory("/workspace/dirA", "", "");
        System.out.println("created: "+created);
        assertNotNull(created);

        String list = tools.fs_list_directory("/workspace", "", "");
        System.out.println("list: "+list);
        assertNotNull(list);

        String tree = tools.fs_directory_tree("/workspace", "", "");
        System.out.println("tree: "+tree);
        assertNotNull(tree);
    }

    @Test
    void testMoveSearchInfoAllowed() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String write = tools.fs_write_file("/workspace/test.txt", "hello", "", "");
        System.out.println("write: "+write);
        assertNotNull(write);

        String moved = tools.fs_move_file("/workspace/test.txt", "/workspace/test-moved.txt", "", "");
        System.out.println("moved: "+moved);
        assertNotNull(moved);

        String search = tools.fs_search_files("/workspace", "test-moved.txt", null, "", "");
        System.out.println("search: "+search);
        assertNotNull(search);

        String info = tools.fs_get_file_info("/workspace/test-moved.txt", "", "");
        System.out.println("info: "+info);
        assertNotNull(info);

        String allowed = tools.fs_list_allowed_directories("", "");
        System.out.println("allowed: "+allowed);
        assertNotNull(allowed);
    }
}


