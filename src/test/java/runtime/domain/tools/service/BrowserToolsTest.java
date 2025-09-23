package runtime.domain.tools.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

public class BrowserToolsTest extends BaseSandboxTest {

    @Test
    void testNavigateAndSnapshot() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String snap = tools.browser_snapshot();
        System.out.println("Snapshot result: " + snap);
        assertNotNull(snap);
    }

    @Test
    void testTabAndResize() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String snap = tools.browser_snapshot();
        System.out.println("Snapshot result: " + snap);
        assertNotNull(snap);

        String newTab = tools.browser_tab_new(null);
        System.out.println("New tab result: " + newTab);
        assertNotNull(newTab);

        String select = tools.browser_tab_select(0);
        System.out.println("Select tab result: " + select);
        assertNotNull(select);

        String resize = tools.browser_resize(1200.0, 800.0);
        System.out.println("Resize result: " + resize);
        assertNotNull(resize);

        String close = tools.browser_close();
        System.out.println("Close result: " + close);
        assertNotNull(close);
    }

    @Test
    void testConsoleMessagesAndNetworkRequests() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // 测试控制台消息
        String consoleMessages = tools.browser_console_messages_tool();
        System.out.println("Console messages result: " + consoleMessages);
        assertNotNull(consoleMessages);

        // 测试网络请求
        String networkRequests = tools.browser_network_requests();
        System.out.println("Network requests result: " + networkRequests);
        assertNotNull(networkRequests);
    }

    @Test
    void testNavigationControls() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 先导航到一个页面
        String nav1 = tools.browser_navigate("https://cn.bing.com");
        System.out.println("First navigation result: " + nav1);
        assertNotNull(nav1);

        // 再导航到另一个页面
        String nav2 = tools.browser_navigate("https://www.baidu.com");
        System.out.println("Second navigation result: " + nav2);
        assertNotNull(nav2);

        // 测试后退
        String back = tools.browser_navigate_back();
        System.out.println("Navigate back result: " + back);
        assertNotNull(back);

        // 测试前进
        String forward = tools.browser_navigate_forward();
        System.out.println("Navigate forward result: " + forward);
        assertNotNull(forward);
    }

    @Test
    void testScreenshotAndPdf() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // 等待页面加载
        String wait = tools.browser_wait_for(2.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // 测试截图
        String screenshot = tools.browser_take_screenshot(false, "test-screenshot.jpg", null, null);
        System.out.println("Screenshot result: " + screenshot);
        assertNotNull(screenshot);

        // 测试PDF保存
        String pdf = tools.browser_pdf_save("test-page.pdf");
        System.out.println("PDF save result: " + pdf);
        assertNotNull(pdf);
    }

    @Test
    void testTabManagement() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 创建多个标签页
        String nav1 = tools.browser_navigate("https://cn.bing.com");
        System.out.println("First navigation result: " + nav1);
        assertNotNull(nav1);

        String newTab1 = tools.browser_tab_new("https://www.baidu.com");
        System.out.println("New tab 1 result: " + newTab1);
        assertNotNull(newTab1);

        String newTab2 = tools.browser_tab_new("https://www.google.com");
        System.out.println("New tab 2 result: " + newTab2);
        assertNotNull(newTab2);

        // 测试标签页列表
        String tabList = tools.browser_tab_list();
        System.out.println("Tab list result: " + tabList);
        assertNotNull(tabList);

        // 测试选择标签页
        String select = tools.browser_tab_select(1);
        System.out.println("Select tab result: " + select);
        assertNotNull(select);

        // 测试关闭标签页
        String close = tools.browser_tab_close(2);
        System.out.println("Close tab result: " + close);
        assertNotNull(close);
    }

    @Test
    void testKeyboardAndDialog() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // 测试按键操作
        String pressKey = tools.browser_press_key("Tab");
        System.out.println("Press key result: " + pressKey);
        assertNotNull(pressKey);

        // 测试对话框处理（如果有的话）
        String handleDialog = tools.browser_handle_dialog(true, null);
        System.out.println("Handle dialog result: " + handleDialog);
        assertNotNull(handleDialog);
    }

    @Test
    void testFileUpload() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 导航到一个可能有文件上传功能的页面
        String nav = tools.browser_navigate("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_type_file");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // 等待页面加载
        String wait = tools.browser_wait_for(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // 测试文件上传（这里只是测试API调用，实际文件可能不存在）
        String[] testFiles = {"/tmp/test.txt"};
        String upload = tools.browser_file_upload(testFiles);
        System.out.println("File upload result: " + upload);
        assertNotNull(upload);
    }
}


