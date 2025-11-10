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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test browser tools that require page element interaction
 * These tests require pages with specific elements to work properly
 */
public class BrowserInteractiveToolsTest {

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
                // Clean up all test-created sandboxes
                sandboxManager.cleanupAllSandboxes();
                System.out.println("All test sandboxes cleaned up successfully");
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }

    @Test
    void testClickAndType() {
        // Navigate to a page with input fields
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = sandbox.waitFor(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Get page snapshot to obtain element references
        String snapshot = sandbox.snapshot();
        System.out.println("Snapshot result: " + snapshot);
        assertNotNull(snapshot);

        // Note: These tests require real element references from snapshots
        // This is just testing API calls, actual usage requires parsing snapshots to get element references
        try {
            // Test click (using mock element references)
            String click = sandbox.click("search input", "mock-ref-1");
            System.out.println("Click result: " + click);
            assertNotNull(click);
        } catch (Exception e) {
            System.out.println("Click test failed (expected for mock ref): " + e.getMessage());
        }

        try {
            // Test input (using mock element references)
            String type = sandbox.type("search input", "mock-ref-1", "test search", false, false);
            System.out.println("Type result: " + type);
            assertNotNull(type);
        } catch (Exception e) {
            System.out.println("Type test failed (expected for mock ref): " + e.getMessage());
        }
    }

    @Test
    void testHoverAndDrag() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = sandbox.waitFor(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Get page snapshot
        String snapshot = sandbox.snapshot();
        System.out.println("Snapshot result: " + snapshot);
        assertNotNull(snapshot);

        try {
            // Test hover (using mock element references)
            String hover = sandbox.hover("search button", "musCard");
            System.out.println("Hover result: " + hover);
            assertNotNull(hover);
        } catch (Exception e) {
            System.out.println("Hover test failed (expected for mock ref): " + e.getMessage());
        }

        try {
            // Test drag (using mock element references)
            String drag = sandbox.drag("source element", "mock-ref-3", "target element", "mock-ref-4");
            System.out.println("Drag result: " + drag);
            assertNotNull(drag);
        } catch (Exception e) {
            System.out.println("Drag test failed (expected for mock ref): " + e.getMessage());
        }
    }

    @Test
    void testSelectOption() {
        // Navigate to a page with dropdown selection
        String nav = sandbox.navigate("https://www.w3schools.com/tags/tryit.asp?filename=tryhtml5_select");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = sandbox.waitFor(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Get page snapshot
        String snapshot = sandbox.snapshot();
        System.out.println("Snapshot result: " + snapshot);
        assertNotNull(snapshot);

        try {
            // Test dropdown selection (using mock element references)
            String[] options = {"option1", "option2"};
            String select = sandbox.selectOption("select element", "mock-ref-5", options);
            System.out.println("Select option result: " + select);
            assertNotNull(select);
        } catch (Exception e) {
            System.out.println("Select option test failed (expected for mock ref): " + e.getMessage());
        }
    }

    @Test
    void testWaitForText() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Test waiting for text to appear
        String waitForText = sandbox.waitFor(null, "Bing", null);
        System.out.println("Wait for text result: " + waitForText);
        assertNotNull(waitForText);

        // Test waiting for text to disappear
        String waitForTextGone = sandbox.waitFor(null, null, "Loading");
        System.out.println("Wait for text gone result: " + waitForTextGone);
        assertNotNull(waitForTextGone);
    }

    @Test
    void testScreenshotWithElement() {
        // Navigate to a page
        String nav = sandbox.navigate("https://cn.bing.com");
        System.out.println("Navigation result: " + nav);
        assertNotNull(nav);

        // Wait for page to load
        String wait = sandbox.waitFor(3.0, null, null);
        System.out.println("Wait result: " + wait);
        assertNotNull(wait);

        // Test element screenshot (using mock element references)
        try {
            String elementScreenshot = sandbox.takeScreenshot(false, "element-screenshot.jpg", "search input", "mock-ref-6");
            System.out.println("Element screenshot result: " + elementScreenshot);
            assertNotNull(elementScreenshot);
        } catch (Exception e) {
            System.out.println("Element screenshot test failed (expected for mock ref): " + e.getMessage());
        }
    }
}
