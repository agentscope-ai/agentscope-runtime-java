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

package io.agentscope.runtime.sandbox.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BFCLSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@EnabledIfEnvironmentVariable(named = "KUBECONFIG_PATH", matches = ".+")
public class BfclSandboxTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private SandboxManager sandboxManager;

    // Test data constants corresponding to Python ASSISTANT_MESSAGES
    private static final List<Map<String, Object>> ASSISTANT_MESSAGES = List.of(
        // Turn-1
        Map.of("role", "assistant", "content",
                """
                        <tool_call>
                        {"name": "cd", "arguments": {"folder": "document"}}
                        </tool_call>
                        <tool_call>
                        {"name": "mkdir", "arguments": {"dir_name": "temp"}}
                        </tool_call>
                        <tool_call>
                        {"name": "mv", "arguments": {"source": "final_report.pdf", "destination": "temp"}}
                        </tool_call>"""),
        Map.of("role", "assistant", "content", "ok.1"),
        
        // Turn-2
        Map.of("role", "assistant", "content",
                """
                        <tool_call>
                        {"name": "cd", "arguments": {"folder": "temp"}}
                        </tool_call>
                        <tool_call>
                        {"name": "grep", "arguments": {"file_name": "final_report.pdf", "pattern": "budget analysis"}}
                        </tool_call>"""),
        Map.of("role", "assistant", "content", "ok.2"),
        
        // Turn-3
        Map.of("role", "assistant", "content",
            "<tool_call>\n{\"name\": \"sort\", \"arguments\": {\"file_name\": \"final_report.pdf\"}}\n</tool_call>"),
        Map.of("role", "assistant", "content", "ok.2"),
        
        // Turn-4
        Map.of("role", "assistant", "content",
                """
                        <tool_call>
                        {"name": "cd", "arguments": {"folder": ".."}}
                        </tool_call>
                        <tool_call>
                        {"name": "mv", "arguments": {"source": "previous_report.pdf", "destination": "temp"}}
                        </tool_call>
                        <tool_call>
                        {"name": "cd", "arguments": {"folder": "temp"}}
                        </tool_call>
                        <tool_call>
                        {"name": "diff", "arguments": {"file_name1": "final_report.pdf", "file_name2": "previous_report.pdf"}}
                        </tool_call>"""),
        Map.of("role", "assistant", "content", "ok.2")
    );

    @BeforeEach
    void setUp() {
        // Initialize sandbox manager
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.OFF);
        try {
            BaseClientConfig clientConfig = KubernetesClientConfig.builder().kubeConfigPath(System.getenv("KUBECONFIG_PATH")).build();
            ManagerConfig config = new ManagerConfig.Builder()
                    .containerDeployment(clientConfig)
                    .build();
            sandboxManager = new SandboxManager(config);
            System.out.println("SandboxManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize SandboxManager: " + e.getMessage());
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
    public void testBfclSandbox() throws JsonProcessingException {
        try(BFCLSandbox bfclSandbox = new BFCLSandbox(sandboxManager, "test-user", "test-session")) {
            String envProfiles = bfclSandbox.getEnvProfile("bfcl", "train", null);
            System.out.println("BFCLSandbox env profiles: " + envProfiles);
            assertNotNull(envProfiles);

            ObjectMapper mapper = new ObjectMapper();
            List<String> profileList = mapper.readValue(envProfiles, new TypeReference<List<String>>() {});

            String initResponse = bfclSandbox.createInstance("bfcl", profileList.get(0), null, Map.of("model_name", "gt-script"));
            System.out.println("Init state: " + initResponse);
            assertNotNull(initResponse);

            // Parse instance ID and query from response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = mapper.readValue(initResponse, Map.class);
            String instanceId = extractInstanceId(initResponse);
            String query = responseMap.get("state").toString();

            System.out.println("Created instance " + instanceId + " with query: " + query);
            assertNotNull(instanceId);
            assertNotNull(query);

            // Execute steps (turns)
            for (int turnNo = 0; turnNo < ASSISTANT_MESSAGES.size(); turnNo++) {
                Map<String, Object> message = ASSISTANT_MESSAGES.get(turnNo);
                String result = bfclSandbox.step(instanceId, message, null);
                System.out.println("\n[TURN " + (turnNo + 1) + "] Step result: " + result);
                assertNotNull(result);

                // Parse result to check if terminated
                @SuppressWarnings("unchecked")
                Map<String, Object> stepResult = mapper.readValue(result, Map.class);
                Boolean isTerminated = (Boolean) stepResult.get("is_terminated");
                if (isTerminated != null && isTerminated) {
                    System.out.println("Terminated at turn " + (turnNo + 1));
                    break;
                }
            }

            // Evaluate
            String score = bfclSandbox.evaluate(instanceId, Map.of(), Map.of("sparse", false));
            System.out.println("\n[RESULT] sparse_score = " + score);
            assertNotNull(score);

            // Release instance
            String releaseResult = bfclSandbox.releaseInstance(instanceId);
            System.out.println("[DONE] released instance: " + releaseResult);
            assertNotNull(releaseResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractInstanceId(String response) {
        // Simple extraction of instance_id from response
        // This is a basic implementation - you might need to adjust based on actual response format
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = mapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) responseMap.get("info");
            if (info != null) {
                return info.get("instance_id").toString();
            }
        } catch (Exception e) {
            System.err.println("Error extracting instance ID: " + e.getMessage());
        }
        return null;
    }
}
