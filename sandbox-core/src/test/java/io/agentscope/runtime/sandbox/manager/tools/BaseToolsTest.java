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

package io.agentscope.runtime.sandbox.manager.tools;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
public class BaseToolsTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private SandboxService sandboxService;
    private BaseSandbox sandbox;

    @BeforeEach
    void setUp() {
        // Initialize sandbox manager
        try {
            BaseClientStarter clientConfig = DockerClientStarter.builder().build();
            ManagerConfig managerConfig = ManagerConfig.builder()
                    .clientConfig(clientConfig)
                    .build();
            SandboxService sandboxService = new SandboxService(managerConfig);
            sandboxService.start();
            sandbox = new BaseSandbox(sandboxService, "test-user", "test-session");
            System.out.println("SandboxService initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize SandboxService: " + e.getMessage());
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
        if (sandboxService != null) {
            try {
                // Clean up all test-created sandboxes
                sandboxService.cleanupAllSandboxes();
                System.out.println("All test sandboxes cleaned up successfully");
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }


    @Test
    void testRunPythonAndShell() {
        // Test Python execution
        String py = sandbox.runIpythonCell("print(1+1)");
        System.out.println("Python output: " + py);
        assertNotNull(py);
        assertTrue(py.contains("2") || py.contains("success"), "Python should execute successfully");

        // Test Shell command execution
        String sh = sandbox.runShellCommand("echo hello");
        System.out.println("Shell output: " + sh);
        assertNotNull(sh);
        assertTrue(sh.contains("hello") || sh.contains("success"), "Shell command should execute successfully");
    }
}


