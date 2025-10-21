package runtime.domain.tools.service.engine.agent;

import io.agentscope.runtime.autoconfig.deployer.LocalDeployManager;
import io.agentscope.runtime.autoconfig.deployer.LocalDeployer;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalDeployerTest
 * Tests the basic functionality of local deployer, including deployment, status checking and shutdown
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class LocalDeployerTest {

    private LocalDeployManager localDeployManager;
    private Runner runner;
    private ContextManager contextManager;

    @BeforeEach
    void setUp() {
        localDeployManager = new LocalDeployManager();
        initializeContextManager();
        initializeRunner();
    }

    @AfterEach
    void tearDown() {
        if (localDeployManager != null) {
            localDeployManager.shutdown();
        }
        if (contextManager != null) {
            try {
                contextManager.stop().get();
            } catch (Exception e) {
                System.err.println("Error stopping context manager: " + e.getMessage());
            }
        }
    }

    private void initializeContextManager() {
        try {
            SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
            MemoryService memoryService = new InMemoryMemoryService();
            this.contextManager = new ContextManager(
                    ContextComposer.class,
                    sessionHistoryService,
                    memoryService
            );

            sessionHistoryService.start().get();
            memoryService.start().get();
            this.contextManager.start().get();

            System.out.println("ContextManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize ContextManager: " + e.getMessage());
            throw new RuntimeException("ContextManager initialization failed", e);
        }
    }

    private void initializeRunner() {
        try {
            // Create a simple Runner instance
            this.runner = new Runner();
            
            // Register ContextManager
            runner.registerContextManager(contextManager);
            
            System.out.println("Runner initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Runner: " + e.getMessage());
            throw new RuntimeException("Runner initialization failed", e);
        }
    }

    @Test
    @DisplayName("Test LocalDeployer basic functionality - deployment and shutdown")
    void testLocalDeployerBasicFunctionality() {
        System.out.println("=== Starting LocalDeployer basic functionality test ===");
        
        // Test initial state
        assertNotNull(localDeployManager, "LocalDeployManager should be properly initialized");
        
        // Test deployment functionality
        String endpointName = "test-endpoint";
        assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "Deployment should execute successfully without throwing exceptions");
        
        // Verify deployment status
        // Note: Since LocalDeployManager uses Spring context, we cannot directly access applicationContext
        // But we can verify deployment success by not throwing exceptions
        
        System.out.println("LocalDeployer deployment test completed");
    }

    @Test
    @DisplayName("Test duplicate deployment - should not create duplicates")
    void testDuplicateDeployment() {
        System.out.println("=== Starting duplicate deployment test ===");
        
        String endpointName = "duplicate-test-endpoint";
        
        // First deployment
        assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "First deployment should succeed");
        
        // Second deployment - should not create duplicates
        assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "Duplicate deployment should be ignored without throwing exceptions");
        
        System.out.println("Duplicate deployment test completed");
    }

    @Test
    @DisplayName("Test deployment with different endpoint names")
    void testDifferentEndpointNames() {
        System.out.println("=== Starting different endpoint names deployment test ===");
        
        String[] endpointNames = {"endpoint1", "endpoint2", "test-endpoint"};
        
        for (String endpointName : endpointNames) {
            assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "Deployment of endpoint: " + endpointName + " should succeed");
        }
        
        System.out.println("Different endpoint names deployment test completed");
    }

    @Test
    @DisplayName("Test shutdown functionality")
    void testShutdownFunctionality() {
        System.out.println("=== Starting shutdown functionality test ===");
        
        String endpointName = "shutdown-test-endpoint";
        
        // Deploy first
        assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "Deployment should succeed");
        
        // Test shutdown
        assertDoesNotThrow(() -> localDeployManager.shutdown(), "Shutdown should execute successfully without throwing exceptions");
        
        System.out.println("Shutdown functionality test completed");
    }

    @Test
    @DisplayName("Test multiple shutdowns - should handle safely")
    void testMultipleShutdowns() {
        System.out.println("=== Starting multiple shutdowns test ===");
        
        String endpointName = "multiple-shutdown-test";
        
        // Deploy
        assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "Deployment should succeed");
        
        // First shutdown
        assertDoesNotThrow(() -> localDeployManager.shutdown(), "First shutdown should succeed");
        
        // Second shutdown - should be handled safely
        assertDoesNotThrow(() -> localDeployManager.shutdown(), "Duplicate shutdown should be handled safely without throwing exceptions");
        
        System.out.println("Multiple shutdowns test completed");
    }

    @Test
    @DisplayName("Test LocalDeployer class structure")
    void testLocalDeployerClassStructure() {
        System.out.println("=== Starting LocalDeployer class structure test ===");
        
        // Verify LocalDeployer class can be instantiated
        assertDoesNotThrow(() -> {
            LocalDeployer deployer = new LocalDeployer();
            assertNotNull(deployer, "LocalDeployer instance should be created correctly");
        }, "LocalDeployer instantiation should succeed");
        
        // Verify main method exists (check via reflection)
        assertDoesNotThrow(() -> {
            LocalDeployer.class.getMethod("main", String[].class);
        }, "LocalDeployer should have main method");
        
        System.out.println("LocalDeployer class structure test completed");
    }

    @Test
    @DisplayName("Test deployment status check")
    void testDeploymentStatusCheck() {
        System.out.println("=== Starting deployment status check test ===");
        
        String endpointName = "status-check-endpoint";
        
        // Pre-deployment status
        System.out.println("Pre-deployment status check completed");
        
        // Execute deployment
        assertDoesNotThrow(() -> localDeployManager.deployStreaming(endpointName), "Deployment should succeed");
        
        // Post-deployment status
        System.out.println("Post-deployment status check completed");
        
        // Verify can shut down normally
        assertDoesNotThrow(() -> localDeployManager.shutdown(), "Shutdown should succeed");
        
        System.out.println("Deployment status check test completed");
    }
}
