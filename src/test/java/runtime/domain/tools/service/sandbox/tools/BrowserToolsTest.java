package runtime.domain.tools.service.sandbox.tools;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

public class BrowserToolsTest{

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
    void testNavigateAndSnapshot() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String nav = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String snap = tools.browser_snapshot("", "");
        System.out.println("Snapshot result: " + snap);
        assertNotNull(snap);
    }

    @Test
    void testTabAndResize() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String nav = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String snap = tools.browser_snapshot("", "");
        System.out.println("Snapshot result: " + snap);
        assertNotNull(snap);

        String newTab = tools.browser_tab_new(null, "", "");
        System.out.println("New tab result: " + newTab);
        assertNotNull(newTab);

        String select = tools.browser_tab_select(0, "", "");
        System.out.println("Select tab result: " + select);
        assertNotNull(select);

        String resize = tools.browser_resize(1200.0, 800.0, "", "");
        System.out.println("Resize result: " + resize);
        assertNotNull(resize);

        String close = tools.browser_close("", "");
        System.out.println("Close result: " + close);
        assertNotNull(close);
    }

    @Test
    void testConsoleMessagesAndNetworkRequests() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String nav = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Test console messages
        String consoleMessages = tools.browser_console_messages_tool("", "");
        System.out.println("Console messages result: " + consoleMessages);
        assertNotNull(consoleMessages);

        // Test network requests
        String networkRequests = tools.browser_network_requests("", "");
        System.out.println("Network requests result: " + networkRequests);
        assertNotNull(networkRequests);
    }

    @Test
    void testNavigationControls() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        // First navigate to a page
        String nav1 = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("First navigation result: " + nav1);
        assertNotNull(nav1);

        // Then navigate to another page
        String nav2 = tools.browser_navigate("https://www.baidu.com", "", "");
        System.out.println("Second navigation result: " + nav2);
        assertNotNull(nav2);

        // Test back navigation
        String back = tools.browser_navigate_back("", "");
        System.out.println("Navigate back result: " + back);
        assertNotNull(back);

        // Test forward navigation
        String forward = tools.browser_navigate_forward("", "");
        System.out.println("Navigate forward result: " + forward);
        assertNotNull(forward);
    }

    @Test
    void testScreenshotAndPdf() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String nav = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = tools.browser_wait_for(2.0, null, null, "", "");
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Test screenshot
        String screenshot = tools.browser_take_screenshot(false, "test-screenshot.jpg", null, null, "", "");
        System.out.println("Screenshot result: " + screenshot);
        assertNotNull(screenshot);

        // Test PDF save
        String pdf = tools.browser_pdf_save("test-page.pdf", "", "");
        System.out.println("PDF save result: " + pdf);
        assertNotNull(pdf);
    }

    @Test
    void testTabManagement() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        // Create multiple tabs
        String nav1 = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("First navigation result: " + nav1);
        assertNotNull(nav1);

        String newTab1 = tools.browser_tab_new("https://www.baidu.com", "", "");
        System.out.println("New tab 1 result: " + newTab1);
        assertNotNull(newTab1);

        String newTab2 = tools.browser_tab_new("https://www.google.com", "", "");
        System.out.println("New tab 2 result: " + newTab2);
        assertNotNull(newTab2);

        // Test tab list
        String tabList = tools.browser_tab_list("", "");
        System.out.println("Tab list result: " + tabList);
        assertNotNull(tabList);

        // Test tab selection
        String select = tools.browser_tab_select(1, "", "");
        System.out.println("Select tab result: " + select);
        assertNotNull(select);

        // Test tab close
        String close = tools.browser_tab_close(2, "", "");
        System.out.println("Close tab result: " + close);
        assertNotNull(close);
    }

    @Test
    void testKeyboardAndDialog() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String nav = tools.browser_navigate("https://cn.bing.com", "", "");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Test key press operations
        String pressKey = tools.browser_press_key("Tab", "", "");
        System.out.println("Press key result: " + pressKey);
        assertNotNull(pressKey);

        // Test dialog handling (if any
        String handleDialog = tools.browser_handle_dialog(true, null, "", "");
        System.out.println("Handle dialog result: " + handleDialog);
        assertNotNull(handleDialog);
    }

    @Test
    void testFileUpload() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        // Navigate to a page that may have file upload functionality
        String nav = tools.browser_navigate("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_type_file", "", "");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = tools.browser_wait_for(3.0, null, null, "", "");
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Test file upload (this is just testing API calls, actual files may not exist)
        String[] testFiles = {"/tmp/test.txt"};
        String upload = tools.browser_file_upload(testFiles, "", "");
        System.out.println("File upload result: " + upload);
        assertNotNull(upload);
    }
}


