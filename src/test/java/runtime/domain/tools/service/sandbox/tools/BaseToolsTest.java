package runtime.domain.tools.service.sandbox.tools;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

public class BaseToolsTest {

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
    void testRunPythonAndShell() {
        SandboxTools tools = new SandboxTools(sandboxManager);

        String py = tools.run_ipython_cell("print(1+1)","","");
        System.out.println("Python output: " + py);
        assertNotNull(py);

        String sh = tools.run_shell_command("echo hello", "", "");
        System.out.println("Shell output: " + sh);
        assertNotNull(sh);
    }
}


