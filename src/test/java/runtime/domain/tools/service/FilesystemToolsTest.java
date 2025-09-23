package runtime.domain.tools.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

public class FilesystemToolsTest extends BaseSandboxTest {

    @Test
    void testReadWriteAndEditFile() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.FILESYSTEM);
        SandboxTools tools = new SandboxTools();

        String write = tools.fs_write_file("/workspace/test.txt", "hello");
        System.out.println("write: "+write);
        assertNotNull(write);

        String read = tools.fs_read_file("/workspace/test.txt");
        System.out.println("read: "+read);
        assertNotNull(read);

        Object[] edits = new Object[] { new java.util.HashMap<String, Object>() {{ put("oldText", "hello"); put("newText", "world"); }} };
        String edited = tools.fs_edit_file("/workspace/test.txt", edits);
        System.out.println("edited: "+edited);
        assertNotNull(edited);
    }

    @Test
    void testDirectoryOps() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.FILESYSTEM);
        SandboxTools tools = new SandboxTools();

        String created = tools.fs_create_directory("/workspace/dirA");
        System.out.println("created: "+created);
        assertNotNull(created);

        String list = tools.fs_list_directory("/workspace");
        System.out.println("list: "+list);
        assertNotNull(list);

        String tree = tools.fs_directory_tree("/workspace");
        System.out.println("tree: "+tree);
        assertNotNull(tree);
    }

    @Test
    void testMoveSearchInfoAllowed() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.FILESYSTEM);
        SandboxTools tools = new SandboxTools();

        String write = tools.fs_write_file("/workspace/test.txt", "hello");
        System.out.println("write: "+write);
        assertNotNull(write);

        String moved = tools.fs_move_file("/workspace/test.txt", "/workspace/test-moved.txt");
        System.out.println("moved: "+moved);
        assertNotNull(moved);

        String search = tools.fs_search_files("/workspace", "test-moved.txt", null);
        System.out.println("search: "+search);
        assertNotNull(search);

        String info = tools.fs_get_file_info("/workspace/test-moved.txt");
        System.out.println("info: "+info);
        assertNotNull(info);

        String allowed = tools.fs_list_allowed_directories();
        System.out.println("allowed: "+allowed);
        assertNotNull(allowed);
    }
}


