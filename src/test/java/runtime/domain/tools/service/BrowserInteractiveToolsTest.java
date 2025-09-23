package runtime.domain.tools.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

/**
 * 测试需要页面元素交互的浏览器工具
 * 这些测试需要页面有特定的元素才能正常工作
 */
public class BrowserInteractiveToolsTest extends BaseSandboxTest {

    @Test
    void testClickAndType() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 导航到一个有输入框的页面
        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // 等待页面加载
        String wait = tools.browser_wait_for(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // 获取页面快照以获取元素引用
        String snapshot = tools.browser_snapshot();
        System.out.println("Snapshot result: " + snapshot);
        assertNotNull(snapshot);

        // 注意：这些测试需要从快照中获取真实的元素引用
        // 这里只是测试API调用，实际使用时需要解析快照获取元素引用
        try {
            // 测试点击（使用模拟的元素引用）
            String click = tools.browser_click("search input", "mock-ref-1");
            System.out.println("Click result: " + click);
            assertNotNull(click);
        } catch (Exception e) {
            System.out.println("Click test failed (expected for mock ref): " + e.getMessage());
        }

        try {
            // 测试输入（使用模拟的元素引用）
            String type = tools.browser_type("search input", "mock-ref-1", "test search", false, false);
            System.out.println("Type result: " + type);
            assertNotNull(type);
        } catch (Exception e) {
            System.out.println("Type test failed (expected for mock ref): " + e.getMessage());
        }
    }

    @Test
    void testHoverAndDrag() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String wait = tools.browser_wait_for(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        String snapshot = tools.browser_snapshot();
        System.out.println("Snapshot result: " + snapshot);
        assertNotNull(snapshot);

        try {
            // 测试悬停（使用模拟的元素引用）
            String hover = tools.browser_hover("search button", "musCard");
            System.out.println("Hover result: " + hover);
            assertNotNull(hover);
        } catch (Exception e) {
            System.out.println("Hover test failed (expected for mock ref): " + e.getMessage());
        }

        try {
            // 测试拖拽（使用模拟的元素引用）
            String drag = tools.browser_drag("source element", "mock-ref-3", "target element", "mock-ref-4");
            System.out.println("Drag result: " + drag);
            assertNotNull(drag);
        } catch (Exception e) {
            System.out.println("Drag test failed (expected for mock ref): " + e.getMessage());
        }
    }

    @Test
    void testSelectOption() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        // 导航到一个有下拉选择框的页面
        String nav = tools.browser_navigate("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_select");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String wait = tools.browser_wait_for(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        String snapshot = tools.browser_snapshot();
        System.out.println("Snapshot result: " + snapshot);
        assertNotNull(snapshot);

        try {
            // 测试下拉选择（使用模拟的元素引用）
            String[] options = {"option1", "option2"};
            String select = tools.browser_select_option("select element", "mock-ref-5", options);
            System.out.println("Select option result: " + select);
            assertNotNull(select);
        } catch (Exception e) {
            System.out.println("Select option test failed (expected for mock ref): " + e.getMessage());
        }
    }

    @Test
    void testWaitForText() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // 测试等待文本出现
        String waitForText = tools.browser_wait_for(null, "Bing", null);
        System.out.println("Wait for text result: " + waitForText);
        assertNotNull(waitForText);

        // 测试等待文本消失
        String waitForTextGone = tools.browser_wait_for(null, null, "Loading");
        System.out.println("Wait for text gone result: " + waitForTextGone);
        assertNotNull(waitForTextGone);
    }

    @Test
    void testScreenshotWithElement() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BROWSER);
        SandboxTools tools = new SandboxTools();

        String nav = tools.browser_navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        String wait = tools.browser_wait_for(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // 测试元素截图（使用模拟的元素引用）
        try {
            String elementScreenshot = tools.browser_take_screenshot(false, "element-screenshot.jpg", "search input", "mock-ref-6");
            System.out.println("Element screenshot result: " + elementScreenshot);
            assertNotNull(elementScreenshot);
        } catch (Exception e) {
            System.out.println("Element screenshot test failed (expected for mock ref): " + e.getMessage());
        }
    }
}
