package runtime.domain.tools.service.sandbox.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.TrainingSandboxTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "KUBECONFIG_PATH", matches = ".+")
public class BfclSandboxTest {

    private SandboxManager sandboxManager;
    private TrainingSandboxTools tools;

    // Test data constants corresponding to Python ASSISTANT_MESSAGES
    private static final List<Map<String, String>> ASSISTANT_MESSAGES = List.of(
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
            BaseClientConfig config = new KubernetesClientConfig(System.getenv("KUBECONFIG_PATH"));
            sandboxManager = new SandboxManager(config);
            tools = new TrainingSandboxTools(sandboxManager);
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

    public void testBfclSandbox() throws JsonProcessingException {
        // Get environment profiles
        String envProfiles = tools.getEnvProfiles(SandboxType.BFCL, "bfcl", "train", null, "", "");
        System.out.println("Environment profiles: " + envProfiles);
        assertNotNull(envProfiles);

        // Parse profiles to get task ID (assuming it's the second one like in Python)
        ObjectMapper mapper = new ObjectMapper();
        List<?> profileList = mapper.readValue(envProfiles, List.class);
        String taskId = profileList.get(1).toString();

        // Create instance
        String initResponse = tools.createInstance("bfcl", taskId, null, Map.of("model_name", "gt-script"), "", "");
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
            Map<String, String> message = ASSISTANT_MESSAGES.get(turnNo);
            String result = tools.step(instanceId, message, null, "", "");
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
        String score = tools.evaluate(instanceId, Map.of(), Map.of("sparse", false), "", "");
        System.out.println("\n[RESULT] sparse_score = " + score);
        assertNotNull(score);

        // Release instance
        String releaseResult = tools.releaseInstance(instanceId, "", "");
        System.out.println("[DONE] released instance: " + releaseResult);
        assertNotNull(releaseResult);
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
