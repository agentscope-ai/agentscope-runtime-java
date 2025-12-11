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

package io.agentscope.runtime.sandbox.tools;

import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfDockerAvailable
public class BrowserToolsTest{

    private SandboxManager sandboxManager;
    private BrowserSandbox sandbox;

    @BeforeEach
    void setUp() {
        // Initialize sandbox manager
        try {
            BaseClientConfig clientConfig = DockerClientConfig.builder().build();
            ManagerConfig config = new ManagerConfig.Builder()
                    .containerDeployment(clientConfig)
                    .build();
            sandboxManager = new SandboxManager(config);
            sandboxManager.start();
            sandbox = new BrowserSandbox(sandboxManager, "test-user", "test-session");
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
                sandboxManager.cleanupAllSandboxes();
                System.out.println("All test sandboxes cleaned up successfully");
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }

    @Test
    void testNavigateAndSnapshot() {
        // Test browser navigation
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Test browser snapshot
        String snap = sandbox.snapshot();
        System.out.println("Snapshot result: " + snap);
        assertNotNull(snap);
    }

    @Test
    void testTabAndResize() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Take a snapshot
        String snap = sandbox.snapshot();
        System.out.println("Snapshot result: " + snap);
        assertNotNull(snap);

        // Open new tab
        String newTab = sandbox.tabNew(null);
        System.out.println("New tab result: " + newTab);
        assertNotNull(newTab);

        // Select tab
        String select = sandbox.tabSelect(0);
        System.out.println("Select tab result: " + select);
        assertNotNull(select);

        // Resize browser window
        String resize = sandbox.resize(1200.0, 800.0);
        System.out.println("Resize result: " + resize);
        assertNotNull(resize);

        // Close browser
        String close = sandbox.closeBrowser();
        System.out.println("Close result: " + close);
        assertNotNull(close);
    }

    @Test
    void testConsoleMessagesAndNetworkRequests() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Test console messages
        String consoleMessages = sandbox.consoleMessages();
        System.out.println("Console messages result: " + consoleMessages);
        assertNotNull(consoleMessages);

        // Test network requests
        String networkRequests = sandbox.networkRequests();
        System.out.println("Network requests result: " + networkRequests);
        assertNotNull(networkRequests);
    }

    @Test
    void testNavigationControls() {
        // First navigate to a page
        String nav1 = sandbox.navigate("https://cn.bing.com");
        System.out.println("First navigation result: " + nav1);
        assertNotNull(nav1);

        // Then navigate to another page
        String nav2 = sandbox.navigate("https://www.baidu.com");
        System.out.println("Second navigation result: " + nav2);
        assertNotNull(nav2);

        // Test back navigation
        String back = sandbox.navigateBack();
        System.out.println("Navigate back result: " + back);
        assertNotNull(back);

        // Test forward navigation
        String forward = sandbox.navigateForward();
        System.out.println("Navigate forward result: " + forward);
        assertNotNull(forward);
    }

    @Test
    void testScreenshotAndPdf() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = sandbox.waitFor(2.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Test screenshot
        String screenshot = sandbox.takeScreenshot(false, "test-screenshot.jpg", null, null);
        System.out.println("Screenshot result: " + screenshot);
        assertNotNull(screenshot);

        // Test PDF save
        String pdf = sandbox.pdfSave("test-page.pdf");
        System.out.println("PDF save result: " + pdf);
        assertNotNull(pdf);
    }

    @Test
    void testTabManagement() {
        // Create multiple tabs
        String nav1 = sandbox.navigate("https://cn.bing.com");
        System.out.println("First navigation result: " + nav1);
        assertNotNull(nav1);

        String newTab1 = sandbox.tabNew("https://www.baidu.com");
        System.out.println("New tab 1 result: " + newTab1);
        assertNotNull(newTab1);

        String newTab2 = sandbox.tabNew("https://cn.bing.com");
        System.out.println("New tab 2 result: " + newTab2);
        assertNotNull(newTab2);

        // Test tab list
        String tabList = sandbox.tabList();
        System.out.println("Tab list result: " + tabList);
        assertNotNull(tabList);

        // Test tab selection
        String select = sandbox.tabSelect(1);
        System.out.println("Select tab result: " + select);
        assertNotNull(select);

        // Test tab close
        String close = sandbox.tabClose(2);
        System.out.println("Close tab result: " + close);
        assertNotNull(close);
    }

    @Test
    void testKeyboardAndDialog() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Test key press operations
        String pressKey = sandbox.pressKey("Tab");
        System.out.println("Press key result: " + pressKey);
        assertNotNull(pressKey);

        // Test dialog handling
        String handleDialog = sandbox.handleDialog(true, null);
        System.out.println("Handle dialog result: " + handleDialog);
        assertNotNull(handleDialog);
    }

    @Test
    void testFileUpload() {
        // Navigate to a page that may have file upload functionality
        String nav = sandbox.navigate("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_input_type_file");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = sandbox.waitFor(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Test file upload (this is just testing API calls, actual files may not exist)
        String[] testFiles = {"/tmp/test.txt"};
        String upload = sandbox.fileUpload(testFiles);
        System.out.println("File upload result: " + upload);
        assertNotNull(upload);
    }
}


