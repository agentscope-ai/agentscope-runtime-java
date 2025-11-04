package runtime.domain.tools.service.sandbox.tools;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BaseToolsTest {

    private SandboxManager sandboxManager;
    private BaseSandbox sandbox;

    @BeforeEach
    void setUp() {
        // Initialize sandbox manager
        try {
            BaseClientConfig clientConfig = new DockerClientConfig();
            ManagerConfig config = new ManagerConfig.Builder()
                    .containerDeployment(clientConfig)
                    .build();
            sandboxManager = new SandboxManager(config);
            sandbox = new BaseSandbox(sandboxManager, "test-user", "test-session");
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


