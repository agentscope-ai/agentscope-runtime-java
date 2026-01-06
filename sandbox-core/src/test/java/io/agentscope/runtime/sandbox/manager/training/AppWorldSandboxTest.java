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

package io.agentscope.runtime.sandbox.manager.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@EnabledIfDockerAvailable
public class AppWorldSandboxTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private SandboxService sandboxService;

    @BeforeEach
    void setUp() {
        // Initialize sandbox manager
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.OFF);
        try {
            BaseClientStarter clientConfig = DockerClientStarter.builder().build();
            ManagerConfig config = new ManagerConfig.Builder()
                    .clientConfig(clientConfig)
                    .build();
            sandboxService = new SandboxService(config);
            sandboxService.start();
            System.out.println("SandboxService initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize SandboxService: " + e.getMessage());
            throw new RuntimeException("Failed to initialize test environment", e);
        }
    }

    @AfterEach
    void tearDown() {
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
    public void testEnvProfiles() {
        try(APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "test-user", "test-session")) {
            String profiles = appWorldSandbox.getEnvProfile("appworld", "train", null);
            System.out.println("APPWorldSandbox env profiles: " + profiles);
            assertNotNull(profiles);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInstance() throws JsonProcessingException {
        try(APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "test-user", "test-session")) {
            String initResponse = appWorldSandbox.createInstance("appworld", "82e2fac_1", null, null);
            assertNotNull(initResponse);
            Map<String, Object> dataMap = parseTopLevel(initResponse);
            String instanceId = parseInstanceId(initResponse);
            String query = dataMap.get("state").toString();
            System.out.println("Create instance " + instanceId + " with query: " + query);
            assertNotNull(instanceId);
            assertNotNull(query);

            Map<String, Object> action = Map.of("role", "assistant", "content", "```python\nprint('hello appworld!!')\n```");

            String result = appWorldSandbox.step(instanceId, action, null);
            System.out.println("Step result: " + result);
            assertNotNull(result);

            String score = appWorldSandbox.evaluate(instanceId, Map.of(), Map.of("sparse", true));
            System.out.println("Evaluate score: " + score);
            assertNotNull(score);

            String taskIDs = appWorldSandbox.getTaskIds("appworld", "train", null);
            System.out.println("Task IDs: " + taskIDs);
            assertNotNull(taskIDs);

            String success = appWorldSandbox.releaseInstance(instanceId);
            System.out.println("Release instance result: " + success);
            assertNotNull(success);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Parse top-level key=value structure into Map<String, Object>
     * - Nested {...} or [...] are preserved as strings
     * - Only processes top-level comma-separated key-value pairs
     *
     * @param input Input string, e.g. "{ a=1, b={x=y}, c=[1,2] }"
     * @return Map<String, Object>
     */
    public static Map<String, Object> parseTopLevel(String input) {
        if (input == null) return new LinkedHashMap<>();

        // Remove leading and trailing braces (if present)
        String trimmed = input.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (trimmed.isEmpty()) return result;

        List<String> pairs = splitTopLevel(trimmed, ',');

        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            // Find the first '=' (not within nested structures)
            int eqIndex = findTopLevelEquals(pair);
            if (eqIndex == -1) {
                // No = found, skip or treat as key=null?
                result.put(pair, null);
                continue;
            }

            String key = pair.substring(0, eqIndex).trim();
            String value = pair.substring(eqIndex + 1).trim();

            // If value is {...} or [...], keep as string (without removing brackets)
            if ((value.startsWith("{") && value.endsWith("}")) || (value.startsWith("[") && value.endsWith("]"))) {
                result.put(key, value); // Keep as is
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    // Find the position of the top-level '=' in the string (not within nested {} or [])
    private static int findTopLevelEquals(String s) {
        int braceCount = 0;   // {}
        int bracketCount = 0; // []
        boolean inQuotes = false;
        char escapeChar = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escapeChar != 0) {
                escapeChar = 0;
                continue;
            }

            if (c == '\\') {
                escapeChar = c;
                continue;
            }

            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                } else {
                    inQuotes = false;
                }
                continue;
            }

            if (inQuotes) continue;

            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            else if (c == '[') bracketCount++;
            else if (c == ']') bracketCount--;
            else if (c == '=' && braceCount == 0 && bracketCount == 0) {
                return i;
            }
        }
        return -1;
    }

    // Split by top-level delimiter (ignoring nested ones)
    private static List<String> splitTopLevel(String s, char delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int braceCount = 0;
        int bracketCount = 0;
        boolean inQuotes = false;
        char escapeChar = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escapeChar != 0) {
                escapeChar = 0;
                continue;
            }

            if (c == '\\') {
                escapeChar = c;
                continue;
            }

            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                } else {
                    inQuotes = false;
                }
                continue;
            }

            if (inQuotes) continue;

            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            else if (c == '[') bracketCount++;
            else if (c == ']') bracketCount--;
            else if (c == delimiter && braceCount == 0 && bracketCount == 0) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start)); // last part
        return parts;
    }

    private String parseInstanceId(String data) {
        int startIndex = data.indexOf("instance_id=");
        if (startIndex == -1) return null;

        startIndex += "instance_id=".length();
        int endIndex = data.indexOf(',', startIndex);
        if (endIndex == -1) {
            endIndex = data.length();
        }

        return data.substring(startIndex, endIndex).trim();
    }
}
