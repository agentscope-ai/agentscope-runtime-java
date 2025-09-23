package runtime.domain.tools.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

public class GenericToolsTest extends BaseSandboxTest {

    @Test
    void testRunPythonAndShell() {
        Assumptions.assumeTrue(TestUtil.shouldRunIntegration());
        recordSandboxUsage(SandboxType.BASE);
        SandboxTools tools = new SandboxTools();

        String py = tools.run_ipython_cell("print(1+1)");
        System.out.println("Python output: " + py);
        assertNotNull(py);

        String sh = tools.run_shell_command("echo hello");
        System.out.println("Shell output: " + sh);
        assertNotNull(sh);
    }
}


