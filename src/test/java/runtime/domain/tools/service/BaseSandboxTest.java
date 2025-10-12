package runtime.domain.tools.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for sandbox tests, automatically manages sandbox lifecycle
 */
public abstract class BaseSandboxTest {
    

    private static final Set<SandboxType> usedSandboxes = ConcurrentHashMap.newKeySet();
    
    // Get shared SandboxManager instance
    private SandboxManager getSandboxManager() {
        try {
            SandboxTools sandboxTools = new SandboxTools();
            return sandboxTools.getSandboxManager();
        } catch (Exception e) {
            System.err.println("Unable to get shared SandboxManager instance: " + e.getMessage());
            return new SandboxManager();
        }
    }
    
    @BeforeEach
    void setUpSandboxManager() {
        System.out.println("Initializing test environment, currently used sandboxes: " + usedSandboxes);
    }
    
    @AfterEach
    void tearDownSandboxes() {
        if (!usedSandboxes.isEmpty()) {
            System.out.println("Starting to clean up sandbox containers...");
            System.out.println("Sandbox types to clean up: " + usedSandboxes);
            
            SandboxManager sandboxManager = getSandboxManager();
            for (SandboxType sandboxType : usedSandboxes) {
                try {
                    System.out.println("Stopping and removing " + sandboxType + " sandbox...");
                    sandboxManager.stopAndRemoveSandbox(sandboxType, "", "");
                    System.out.println(sandboxType + " sandbox successfully deleted");
                } catch (Exception e) {
                    System.err.println("Error deleting " + sandboxType + " sandbox: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            usedSandboxes.clear();
        } else {
            System.out.println("No sandbox containers to clean up");
        }
    }
    
    /**
     * Record used sandbox type for cleanup after test
     * @param sandboxType sandbox type
     */
    protected void recordSandboxUsage(SandboxType sandboxType) {
        usedSandboxes.add(sandboxType);
    }
    
}
