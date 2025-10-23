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
package runtime.domain.tools.service.sandbox.manager;

import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;
import io.agentscope.runtime.sandbox.manager.registry.SandboxAnnotationProcessor;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryInitializer;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Custom Sandbox Test Class
 * Demonstrates how to create and test custom sandboxes using @RegisterSandbox annotation
 */
@DisplayName("Custom Sandbox Registration and Configuration Tests")
public class CustomSandboxTest {

    @RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-python:latest",
        sandboxType = SandboxType.PYTHON,
        securityLevel = "high",
        timeout = 60,
        description = "Custom Python Sandbox with extra security",
        resourceLimits = {"memory=2g", "cpu=2.0"}
    )
    public static class CustomPythonSandbox extends Sandbox {
        public CustomPythonSandbox(
                SandboxManager managerApi,
                String userId,
                String sessionId) {
            super(managerApi, userId, sessionId, SandboxType.PYTHON, 60);
        }
        
        public String executePython(String code) {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("code", code);
            return callTool("run_ipython_cell", arguments);
        }
    }
    
    @RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-custom_sandbox:latest",
        customType = "custom_sandbox",
        securityLevel = "medium",
        timeout = 60,
        description = "my sandbox",
        environment = {
            "TAVILY_API_KEY=${TAVILY_API_KEY}",
            "AMAP_MAPS_API_KEY=${AMAP_MAPS_API_KEY}"
        }
    )
    public static class MyCustomSandbox extends Sandbox {
        public MyCustomSandbox(
                SandboxManager managerApi,
                String userId,
                String sessionId) {
            super(managerApi, userId, sessionId, SandboxType.BASE, 60);
        }
    }
    
    @RegisterSandbox(
        imageName = "my-registry/my-advanced-sandbox:latest",
        customType = "advanced_sandbox",
        securityLevel = "high",
        timeout = 120,
        description = "Advanced custom sandbox with full configuration",
        environment = {
            "API_KEY=${API_KEY}",
            "DEBUG_MODE=true",
            "MAX_WORKERS=4"
        },
        resourceLimits = {
            "memory=4g",
            "cpu=4.0"
        },
        runtimeConfig = {
            "enable_gpu=true",
            "max_connections=100"
        }
    )
    public static class AdvancedCustomSandbox extends Sandbox {
        public AdvancedCustomSandbox(
                SandboxManager managerApi,
                String userId,
                String sessionId) {
            super(managerApi, userId, sessionId, SandboxType.BASE, 120);
        }
        
        public String executeAdvancedTask(String taskName, Map<String, Object> params) {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("task", taskName);
            arguments.putAll(params);
            return callTool("execute_task", arguments);
        }
    }

    @BeforeAll
    @DisplayName("Initialize and register all custom sandboxes")
    public static void setUp() {
        System.out.println("\n=== Initializing Custom Sandbox Tests ===\n");
        System.out.println("Step 1: Initializing Sandbox Registry...");
        SandboxRegistryInitializer.initialize();
        System.out.println("Step 2: Registering custom sandbox classes...");
        SandboxAnnotationProcessor.processClass(CustomPythonSandbox.class);
        SandboxAnnotationProcessor.processClass(MyCustomSandbox.class);
        SandboxAnnotationProcessor.processClass(AdvancedCustomSandbox.class);
        System.out.println("Initialization complete\n");
    }

    @Test
    @DisplayName("Test CustomPythonSandbox registration")
    public void testCustomPythonSandboxRegistration() {
        System.out.println("\n--- Test CustomPythonSandbox Registration ---");

        boolean pythonRegistered = SandboxRegistryService.isRegistered(SandboxType.PYTHON);
        assertTrue(pythonRegistered, "CustomPythonSandbox should be registered");
        System.out.println("✓ CustomPythonSandbox registered");

        SandboxRegistryService.getConfigByType(SandboxType.PYTHON).ifPresent(config -> {
            System.out.println("\nConfiguration:");
            System.out.println("  Image: " + config.getImageName());
            System.out.println("  Type: " + config.getSandboxType());
            System.out.println("  Security Level: " + config.getSecurityLevel());
            System.out.println("  Timeout: " + config.getTimeout() + "s");
            System.out.println("  Description: " + config.getDescription());
            System.out.println("  Resource Limits: " + config.getResourceLimits());

            assertNotNull(config.getImageName());
            assertEquals(SandboxType.PYTHON, config.getSandboxType());
            assertEquals("high", config.getSecurityLevel());
            assertEquals(60, config.getTimeout());
            assertEquals("Custom Python Sandbox with extra security", config.getDescription());
            assertNotNull(config.getResourceLimits());
            assertEquals("2g", config.getResourceLimits().get("memory"));
            Object cpuValue = config.getResourceLimits().get("cpu");
            assertTrue(cpuValue != null && 
                (cpuValue.equals("2.0") || cpuValue.equals(2.0)), 
                "CPU value should be 2.0");
        });
    }

    @Test
    @DisplayName("Test MyCustomSandbox registration (custom type)")
    public void testMyCustomSandboxRegistration() {
        System.out.println("\n--- Test MyCustomSandbox Registration ---");

        boolean customRegistered = SandboxRegistryService.isCustomTypeRegistered("custom_sandbox");
        assertTrue(customRegistered, "MyCustomSandbox should be registered");
        System.out.println("✓ MyCustomSandbox registered (custom type: custom_sandbox)");

        SandboxRegistryService.getCustomTypeConfig("custom_sandbox").ifPresent(config -> {
            System.out.println("\nConfiguration:");
            System.out.println("  Image: " + config.getImageName());
            System.out.println("  Security Level: " + config.getSecurityLevel());
            System.out.println("  Timeout: " + config.getTimeout() + "s");
            System.out.println("  Description: " + config.getDescription());
            System.out.println("  Environment: " + config.getEnvironment());

            assertNotNull(config.getImageName());
            assertTrue(config.getImageName().contains("custom_sandbox"));
            assertEquals("medium", config.getSecurityLevel());
            assertEquals(60, config.getTimeout());
            assertEquals("my sandbox", config.getDescription());
            assertNotNull(config.getEnvironment());
            assertTrue(config.getEnvironment().containsKey("TAVILY_API_KEY"));
            assertTrue(config.getEnvironment().containsKey("AMAP_MAPS_API_KEY"));
        });
    }

    @Test
    @DisplayName("Test AdvancedCustomSandbox registration (full configuration)")
    public void testAdvancedCustomSandboxRegistration() {
        System.out.println("\n--- Test AdvancedCustomSandbox Registration ---");

        boolean advancedRegistered = SandboxRegistryService.isCustomTypeRegistered("advanced_sandbox");
        assertTrue(advancedRegistered, "AdvancedCustomSandbox should be registered");
        System.out.println("✓ AdvancedCustomSandbox registered (custom type: advanced_sandbox)");

        SandboxRegistryService.getCustomTypeConfig("advanced_sandbox").ifPresent(config -> {
            System.out.println("\nConfiguration:");
            System.out.println("  Image: " + config.getImageName());
            System.out.println("  Security Level: " + config.getSecurityLevel());
            System.out.println("  Timeout: " + config.getTimeout() + "s");
            System.out.println("  Description: " + config.getDescription());
            System.out.println("  Environment: " + config.getEnvironment());
            System.out.println("  Resource Limits: " + config.getResourceLimits());
            System.out.println("  Runtime Config: " + config.getRuntimeConfig());

            assertEquals("my-registry/my-advanced-sandbox:latest", config.getImageName());
            assertEquals("high", config.getSecurityLevel());
            assertEquals(120, config.getTimeout());
            assertEquals("Advanced custom sandbox with full configuration", config.getDescription());
            
            assertNotNull(config.getEnvironment());
            assertEquals(3, config.getEnvironment().size());
            assertTrue(config.getEnvironment().containsKey("API_KEY"));
            assertEquals("true", config.getEnvironment().get("DEBUG_MODE"));
            assertEquals("4", config.getEnvironment().get("MAX_WORKERS"));
            
            assertNotNull(config.getResourceLimits());
            assertEquals("4g", config.getResourceLimits().get("memory"));
            Object cpuValue = config.getResourceLimits().get("cpu");
            assertTrue(cpuValue != null && 
                (cpuValue.equals("4.0") || cpuValue.equals(4.0)), 
                "CPU value should be 4.0");
            
            assertNotNull(config.getRuntimeConfig());
            Object enableGpu = config.getRuntimeConfig().get("enable_gpu");
            assertTrue(enableGpu != null && 
                (enableGpu.equals("true") || enableGpu.equals(true)));
            
            Object maxConnections = config.getRuntimeConfig().get("max_connections");
            assertTrue(maxConnections != null && 
                (maxConnections.equals("100") || maxConnections.equals(100)));
            
            assertTrue(config.getRuntimeConfig().containsKey("nano_cpus"));
            assertTrue(config.getRuntimeConfig().containsKey("mem_limit"));
        });
    }

    @Test
    @DisplayName("Test all custom sandboxes registration status")
    public void testAllCustomSandboxesRegistered() {
        System.out.println("\n--- Test All Custom Sandboxes Registration Status ---");

        assertTrue(SandboxRegistryService.isRegistered(SandboxType.PYTHON), 
            "CustomPythonSandbox should be registered");
        assertTrue(SandboxRegistryService.isCustomTypeRegistered("custom_sandbox"), 
            "MyCustomSandbox should be registered");
        assertTrue(SandboxRegistryService.isCustomTypeRegistered("advanced_sandbox"), 
            "AdvancedCustomSandbox should be registered");

        System.out.println("✓ All custom sandboxes successfully registered");

        Map<String, io.agentscope.runtime.sandbox.manager.model.container.SandboxConfig> customTypes = 
            SandboxRegistryService.listAllCustomTypes();
        System.out.println("\nRegistered custom types count: " + customTypes.size());
        customTypes.forEach((name, config) -> 
            System.out.println("  - " + name + " -> " + config.getImageName())
        );
    }

    @Test
    @DisplayName("Test getting custom sandbox image names")
    public void testGetImageNames() {
        System.out.println("\n--- Test Getting Image Names ---");

        SandboxRegistryService.getImageByType(SandboxType.PYTHON).ifPresent(image -> {
            System.out.println("CustomPythonSandbox image: " + image);
            assertFalse(image.isEmpty());
            assertTrue(image.contains("runtime-sandbox-python"));
        });

        SandboxRegistryService.getCustomTypeImage("custom_sandbox").ifPresent(image -> {
            System.out.println("MyCustomSandbox image: " + image);
            assertFalse(image.isEmpty());
        });

        SandboxRegistryService.getCustomTypeImage("advanced_sandbox").ifPresent(image -> {
            System.out.println("AdvancedCustomSandbox image: " + image);
            assertEquals("my-registry/my-advanced-sandbox:latest", image);
        });
    }

    @Test
    @DisplayName("Test configuration override")
    public void testConfigurationOverride() {
        System.out.println("\n--- Test Configuration Override ---");

        SandboxRegistryService.getConfigByType(SandboxType.PYTHON).ifPresent(config -> {
            System.out.println("\nCustom configuration for PYTHON type:");
            System.out.println("  Security Level: " + config.getSecurityLevel());
            System.out.println("  Timeout: " + config.getTimeout());
            
            assertEquals("high", config.getSecurityLevel(), 
                "Custom configuration should override default security level");
            assertEquals(60, config.getTimeout(), 
                "Custom configuration should override default timeout");
        });
    }

    @Test
    @DisplayName("Verify annotation configuration completeness")
    public void testAnnotationConfigurationCompleteness() {
        System.out.println("\n--- Verify Annotation Configuration Completeness ---");

        SandboxRegistryService.getCustomTypeConfig("advanced_sandbox").ifPresentOrElse(
            config -> {
                System.out.println("Verifying AdvancedCustomSandbox configuration...");
                
                assertNotNull(config.getImageName());
                assertNotNull(config.getSecurityLevel());
                assertTrue(config.getTimeout() > 0);
                assertNotNull(config.getDescription());
                assertNotNull(config.getEnvironment());
                assertFalse(config.getEnvironment().isEmpty());
                assertNotNull(config.getResourceLimits());
                assertFalse(config.getResourceLimits().isEmpty());
                assertNotNull(config.getRuntimeConfig());
                assertFalse(config.getRuntimeConfig().isEmpty());
                
                System.out.println("✓ All configuration items verified");
            },
            () -> fail("AdvancedCustomSandbox configuration not found")
        );
    }
}
