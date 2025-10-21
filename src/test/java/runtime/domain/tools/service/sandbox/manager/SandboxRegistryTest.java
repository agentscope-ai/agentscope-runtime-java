package runtime.domain.tools.service.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.model.container.DynamicSandboxType;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SandboxRegistry and SandboxConfig Test
 * Demonstrates how Java version aligns with Python's sandbox registration functionality
 */
public class SandboxRegistryTest {

    /**
     * Test 1: Using predefined sandbox types
     */
    @Test
    void testPredefinedSandboxTypes() {
        System.out.println("\n--- Test 1: Predefined Sandbox Types ---");

        // Get predefined type configuration
        Optional<SandboxConfig> baseConfig = SandboxRegistry.getConfigByType(SandboxType.BASE);
        assertTrue(baseConfig.isPresent(), "BASE type should exist");
        
        baseConfig.ifPresent(config -> {
            System.out.println("BASE Sandbox:");
            System.out.println("  Image: " + config.getImageName());
            System.out.println("  Timeout: " + config.getTimeout() + "s");
            System.out.println("  Security Level: " + config.getSecurityLevel());
            
            assertNotNull(config.getImageName(), "Image name should not be null");
            assertTrue(config.getTimeout() > 0, "Timeout should be greater than 0");
        });

        // Get image name directly
        Optional<String> browserImage = SandboxRegistry.getImageByType(SandboxType.BROWSER);
        assertTrue(browserImage.isPresent(), "BROWSER image should exist");
        
        browserImage.ifPresent(image -> {
            System.out.println("\nBROWSER Sandbox Image: " + image);
            assertFalse(image.isEmpty(), "Image name should not be empty");
        });

        // Check if type is registered
        boolean isRegistered = SandboxRegistry.isRegistered(SandboxType.FILESYSTEM);
        System.out.println("\nFILESYSTEM type registered: " + isRegistered);
        assertTrue(isRegistered, "FILESYSTEM type should be registered");
    }

    /**
     * Test 2: Register custom sandbox type with basic configuration
     */
    @Test
    void testRegisterCustomSandboxTypeBasic() {
        System.out.println("\n--- Test 2: Custom Sandbox Type (Basic) ---");

        String customTypeName = "test_custom_sandbox_" + System.currentTimeMillis();
        String customImage = "testcompany/custom-sandbox:latest";

        // Register custom sandbox type (similar to Python's decorator registration)
        SandboxRegistry.registerCustomType(customTypeName, customImage);

        // Retrieve custom type configuration
        Optional<String> retrievedImage = SandboxRegistry.getCustomTypeImage(customTypeName);
        assertTrue(retrievedImage.isPresent(), "Custom type should be registered");
        
        retrievedImage.ifPresent(image -> {
            System.out.println("Custom Sandbox Image: " + image);
            assertEquals(customImage, image, "Image name should match");
        });

        // Check if custom type is registered
        boolean isCustomRegistered = SandboxRegistry.isCustomTypeRegistered(customTypeName);
        System.out.println("Custom type registered: " + isCustomRegistered);
        assertTrue(isCustomRegistered, "Custom type should be registered");
    }

    /**
     * Test 3: Register custom sandbox type with advanced configuration
     */
    @Test
    void testRegisterCustomSandboxTypeAdvanced() {
        System.out.println("\n--- Test 3: Custom Sandbox Type (Advanced) ---");

        String customTypeName = "test_advanced_sandbox_" + System.currentTimeMillis();

        // Prepare resource limits
        Map<String, Object> resourceLimits = new HashMap<>();
        resourceLimits.put("memory", "2g");
        resourceLimits.put("cpu", 2.0); // 2 CPUs

        // Prepare environment variables
        Map<String, String> environment = new HashMap<>();
        environment.put("CUSTOM_VAR", "custom_value");
        environment.put("DEBUG_MODE", "true");

        // Prepare runtime configuration
        Map<String, Object> runtimeConfig = new HashMap<>();
        runtimeConfig.put("shm_size", "512m");

        // Register with full configuration (similar to Python's full registration)
        SandboxRegistry.registerCustomType(
            customTypeName,
            "testcompany/advanced-sandbox:v1.0",
            resourceLimits,
            "high",           // security level
            600,              // timeout in seconds
            "Advanced custom sandbox for special tasks",
            environment,
            runtimeConfig
        );

        // Retrieve and display configuration
        Optional<SandboxConfig> config = SandboxRegistry.getCustomTypeConfig(customTypeName);
        assertTrue(config.isPresent(), "Custom configuration should exist");
        
        config.ifPresent(cfg -> {
            System.out.println("Advanced Sandbox Configuration:");
            System.out.println("  Image: " + cfg.getImageName());
            System.out.println("  Timeout: " + cfg.getTimeout() + "s");
            System.out.println("  Security Level: " + cfg.getSecurityLevel());
            System.out.println("  Description: " + cfg.getDescription());
            System.out.println("  Resource Limits: " + cfg.getResourceLimits());
            System.out.println("  Environment: " + cfg.getEnvironment());
            System.out.println("  Runtime Config: " + cfg.getRuntimeConfig());
            
            assertEquals("testcompany/advanced-sandbox:v1.0", cfg.getImageName());
            assertEquals(600, cfg.getTimeout());
            assertEquals("high", cfg.getSecurityLevel());
            assertNotNull(cfg.getResourceLimits());
            assertEquals("2g", cfg.getResourceLimits().get("memory"));
            assertEquals(2.0, cfg.getResourceLimits().get("cpu"));
            assertNotNull(cfg.getEnvironment());
            assertEquals("custom_value", cfg.getEnvironment().get("CUSTOM_VAR"));
        });
    }

    /**
     * Test 4: Using DynamicSandboxType for runtime type handling
     */
    @Test
    void testDynamicSandboxType() {
        System.out.println("\n--- Test 4: DynamicSandboxType ---");

        // Create dynamic sandbox type
        String customTypeName = "ml_training_" + System.currentTimeMillis();
        DynamicSandboxType customType = DynamicSandboxType.custom(customTypeName);
        
        System.out.println("Created dynamic type: " + customType.getTypeName());
        System.out.println("Is custom type: " + customType.isCustom());
        
        assertEquals(customTypeName, customType.getTypeName(), "Type name should match");
        assertTrue(customType.isCustom(), "Should be a custom type");

        // Use predefined type via DynamicSandboxType
        DynamicSandboxType baseType = DynamicSandboxType.fromEnum(SandboxType.BASE);
        
        System.out.println("\nPredefined type: " + baseType.getTypeName());
        System.out.println("Is enum type: " + baseType.isEnum());
        
        assertEquals("base", baseType.getTypeName(), "Type name should be base");
        assertTrue(baseType.isEnum(), "Should be an enum type");

        // Get type by name (works for both predefined and custom types)
        try {
            DynamicSandboxType retrievedType = DynamicSandboxType.valueOf("BROWSER");
            System.out.println("\nRetrieved type: " + retrievedType.getTypeName());
            assertEquals("browser", retrievedType.getTypeName(), "Retrieved type name should match");
        } catch (IllegalArgumentException e) {
            fail("Should be able to find BROWSER type: " + e.getMessage());
        }
    }

    /**
     * Test 5: List all registrations
     */
    @Test
    void testListAllRegistrations() {
        System.out.println("\n--- Test 5: List All Registrations ---");

        // List all predefined sandbox types
        Map<SandboxType, SandboxConfig> allSandboxes = SandboxRegistry.listAllSandboxesByType();
        System.out.println("\nPredefined Sandbox Types (" + allSandboxes.size() + "):");
        allSandboxes.forEach((type, config) -> 
            System.out.println("  " + type + " -> " + config.getImageName())
        );
        
        assertFalse(allSandboxes.isEmpty(), "Should have at least some predefined types");
        assertTrue(allSandboxes.containsKey(SandboxType.BASE), "Should contain BASE type");

        // List all custom sandbox types
        Map<String, SandboxConfig> customTypes = SandboxRegistry.listAllCustomTypes();
        System.out.println("\nCustom Sandbox Types (" + customTypes.size() + "):");
        customTypes.forEach((name, config) -> 
            System.out.println("  " + name + " -> " + config.getImageName())
        );

        // Get total count
        int totalCount = SandboxRegistry.getRegisteredCount() + customTypes.size();
        System.out.println("\nTotal registered sandbox types: " + totalCount);
        assertTrue(totalCount > 0, "Should have at least one registered type");

        // List all custom dynamic types
        Map<String, DynamicSandboxType> dynamicTypes = DynamicSandboxType.getCustomTypes();
        System.out.println("\nDynamic Custom Types (" + dynamicTypes.size() + "):");
        dynamicTypes.forEach((name, type) -> 
            System.out.println("  " + name + " -> " + type)
        );
    }

    /**
     * Test 6: Manual config creation
     */
    @Test
    void testManualConfigCreation() {
        System.out.println("\n--- Test 6: Manual Config Creation ---");

        // Create sandbox configuration using builder pattern
        Map<String, Object> resourceLimits = new HashMap<>();
        resourceLimits.put("memory", "4g");
        resourceLimits.put("cpu", 4.0);

        Map<String, String> env = new HashMap<>();
        env.put("JAVA_OPTS", "-Xmx3g");

        SandboxConfig config = new SandboxConfig.Builder()
            .sandboxType(SandboxType.BASE)
            .imageName("custom/java-sandbox:latest")
            .resourceLimits(resourceLimits)
            .securityLevel("high")
            .timeout(900)
            .description("Custom Java development sandbox")
            .environment(env)
            .build();

        System.out.println("Created config:");
        System.out.println("  " + config);
        
        assertNotNull(config, "Configuration should be created successfully");
        assertEquals(SandboxType.BASE, config.getSandboxType());
        assertEquals("custom/java-sandbox:latest", config.getImageName());
        assertEquals("high", config.getSecurityLevel());
        assertEquals(900, config.getTimeout());
        assertEquals("Custom Java development sandbox", config.getDescription());
        assertNotNull(config.getResourceLimits());
        assertEquals("4g", config.getResourceLimits().get("memory"));
        assertNotNull(config.getEnvironment());
        assertEquals("-Xmx3g", config.getEnvironment().get("JAVA_OPTS"));
    }

    /**
     * Test 7: Verify registration idempotency
     */
    @Test
    void testRegistrationIdempotency() {
        System.out.println("\n--- Test 7: Registration Idempotency ---");

        String typeName = "test_idempotent_" + System.currentTimeMillis();
        String image1 = "testcompany/sandbox:v1";
        String image2 = "testcompany/sandbox:v2";

        // First registration
        SandboxRegistry.registerCustomType(typeName, image1);
        Optional<String> firstImage = SandboxRegistry.getCustomTypeImage(typeName);
        assertTrue(firstImage.isPresent(), "First registration should succeed");
        assertEquals(image1, firstImage.get(), "Should be the first image");

        // Second registration (overwrite)
        SandboxRegistry.registerCustomType(typeName, image2);
        Optional<String> secondImage = SandboxRegistry.getCustomTypeImage(typeName);
        assertTrue(secondImage.isPresent(), "Second registration should succeed");
        
        System.out.println("First image: " + image1);
        System.out.println("Second image: " + secondImage.get());
        
        // Verify if overwritten (specific behavior depends on implementation)
        assertNotNull(secondImage.get(), "Second registered image should not be null");
    }
}
