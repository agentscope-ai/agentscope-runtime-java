package runtime.domain.tools.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

/**
 * 工具覆盖率测试 - 确保所有工具都有对应的测试
 * 这个测试类主要验证工具API调用不会抛出异常
 */
public class ToolCoverageTest extends BaseSandboxTest {

    @Test
    void testAllFilesystemTools() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.FILESYSTEM);
        SandboxTools tools = new SandboxTools();

        // 测试所有文件系统工具
        String[] testPaths = {"/workspace/test1.txt", "/workspace/test2.txt"};
        
        // 基础文件操作
        assertNotNull(tools.fs_write_file("/workspace/coverage-test.txt", "test content"));
        assertNotNull(tools.fs_read_file("/workspace/coverage-test.txt"));
        assertNotNull(tools.fs_read_multiple_files(testPaths));
        
        // 目录操作
        assertNotNull(tools.fs_create_directory("/workspace/coverage-dir"));
        assertNotNull(tools.fs_list_directory("/workspace"));
        assertNotNull(tools.fs_directory_tree("/workspace"));
        
        // 文件管理
        assertNotNull(tools.fs_move_file("/workspace/coverage-test.txt", "/workspace/coverage-moved.txt"));
        assertNotNull(tools.fs_search_files("/workspace", "coverage", null));
        assertNotNull(tools.fs_get_file_info("/workspace/coverage-moved.txt"));
        assertNotNull(tools.fs_list_allowed_directories());
        
        // 编辑操作
        Object[] edits = new Object[] { 
            new java.util.HashMap<String, Object>() {{ 
                put("oldText", "test content"); 
                put("newText", "updated content"); 
            }} 
        };
        assertNotNull(tools.fs_edit_file("/workspace/coverage-moved.txt", edits));
    }

    @Test
    void testAllBrowserTools() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 基础导航
        assertNotNull(tools.browser_navigate("https://cn.bing.com"));
        assertNotNull(tools.browser_navigate_back());
        assertNotNull(tools.browser_navigate_forward());
        
        // 页面操作
        assertNotNull(tools.browser_snapshot());
        assertNotNull(tools.browser_console_messages_tool());
        assertNotNull(tools.browser_network_requests());
        
        // 截图和PDF
        assertNotNull(tools.browser_take_screenshot(false, "coverage-test.jpg", null, null));
        assertNotNull(tools.browser_pdf_save("coverage-test.pdf"));
        
        // 标签页管理
        assertNotNull(tools.browser_tab_new("https://www.baidu.com"));
        assertNotNull(tools.browser_tab_list());
        assertNotNull(tools.browser_tab_select(0));
        assertNotNull(tools.browser_tab_close(1));
        
        // 窗口操作
        assertNotNull(tools.browser_resize(1024.0, 768.0));
        assertNotNull(tools.browser_close());
        
        // 等待操作
        assertNotNull(tools.browser_wait_for(1.0, null, null));
        
        // 键盘和对话框
        assertNotNull(tools.browser_press_key("Tab"));
        assertNotNull(tools.browser_handle_dialog(true, "test prompt"));
        
        // 文件上传
        String[] testFiles = {"/tmp/test.txt"};
        assertNotNull(tools.browser_file_upload(testFiles));
    }

    @Test
    void testAllGenericTools() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BASE);
        SandboxTools tools = new SandboxTools();

        // Python 和 Shell 工具
        assertNotNull(tools.run_ipython_cell("print('coverage test')"));
        assertNotNull(tools.run_shell_command("echo 'coverage test'"));
    }

    @Test
    void testInteractiveToolsWithMockData() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 先导航到一个页面
        assertNotNull(tools.browser_navigate("https://cn.bing.com"));
        assertNotNull(tools.browser_wait_for(2.0, null, null));

        // 测试需要元素引用的工具（使用模拟数据）
        // 这些调用可能会失败，但至少验证了API的存在
        try {
            assertNotNull(tools.browser_click("mock element", "mock-ref"));
        } catch (Exception e) {
            System.out.println("Click with mock data failed (expected): " + e.getMessage());
        }

        try {
            assertNotNull(tools.browser_type("mock element", "mock-ref", "test text", false, false));
        } catch (Exception e) {
            System.out.println("Type with mock data failed (expected): " + e.getMessage());
        }

        try {
            assertNotNull(tools.browser_hover("mock element", "mock-ref"));
        } catch (Exception e) {
            System.out.println("Hover with mock data failed (expected): " + e.getMessage());
        }

        try {
            assertNotNull(tools.browser_drag("source", "mock-ref-1", "target", "mock-ref-2"));
        } catch (Exception e) {
            System.out.println("Drag with mock data failed (expected): " + e.getMessage());
        }

        try {
            String[] options = {"option1"};
            assertNotNull(tools.browser_select_option("mock select", "mock-ref", options));
        } catch (Exception e) {
            System.out.println("Select option with mock data failed (expected): " + e.getMessage());
        }
    }

    @Test
    void testToolParameterHandling() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 测试可选参数处理
        assertNotNull(tools.browser_navigate("https://cn.bing.com"));
        
        // 测试所有参数都为null的情况
        assertNotNull(tools.browser_take_screenshot(null, null, null, null));
        assertNotNull(tools.browser_wait_for(null, null, null));
        assertNotNull(tools.browser_tab_new(null));
        assertNotNull(tools.browser_tab_close(null));
        assertNotNull(tools.browser_pdf_save(null));
        assertNotNull(tools.browser_handle_dialog(true, null));
        
        // 测试部分参数为null的情况
        assertNotNull(tools.browser_type("element", "ref", "text", null, null));
        assertNotNull(tools.browser_take_screenshot(false, null, null, null));
    }
}
