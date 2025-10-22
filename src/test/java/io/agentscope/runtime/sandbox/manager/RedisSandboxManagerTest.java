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
package io.agentscope.runtime.sandbox.manager;

import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.RedisManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.fs.LocalFileSystemConfig;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SandboxManager with Redis support
 * Prerequisites:
 * - Redis server must be running on localhost:6379
 * - Docker must be available for container operations
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisSandboxManagerTest {

    private static final String TEST_USER_ID = "test_user";
    private static final String TEST_SESSION_ID = "test_session";
    
    /**
     * Test 1: SandboxManager with Redis enabled
     */
    @Test
    @Order(1)
    @DisplayName("Test SandboxManager with Redis support")
    public void testSandboxManagerWithRedis() {
        // Configure Redis
        RedisManagerConfig redisConfig = RedisManagerConfig.builder()
                .redisServer("localhost")
                .redisPort(6379)
                .redisDb(0)
                .redisPassword(null)
                .redisPortKey("_test_runtime_sandbox_occupied_ports")
                .redisContainerPoolKey("_test_runtime_sandbox_pool")
                .build();
        
        // Configure SandboxManager with Redis enabled
        ManagerConfig config = ManagerConfig.builder()
                .containerPrefixKey("redis_test_")
                .redisConfig(redisConfig)
                .poolSize(2)
                .portRange(50000, 51000)
                .fileSystemConfig(LocalFileSystemConfig.builder()
                        .mountDir("sessions_mount_dir")
                        .build())
                .build();
        
        try (SandboxManager manager = new SandboxManager(config)) {
            assertNotNull(manager, "SandboxManager should be initialized");
            
            // Create a sandbox
            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE, 
                    TEST_USER_ID, 
                    TEST_SESSION_ID
            );
            
            assertNotNull(container, "Container should be created");
            assertNotNull(container.getContainerName(), "Container should have a name");
            assertNotNull(container.getBaseUrl(), "Container should have a base URL");
            assertTrue(container.getBaseUrl().contains("localhost"), "Base URL should contain localhost");
            
            // Verify container is in sandbox map
            Map<SandboxKey, ContainerModel> allSandboxes = manager.getAllSandboxes();
            assertEquals(1, allSandboxes.size(), "Should have 1 sandbox");
            
            // Clean up
            manager.stopAndRemoveSandbox(SandboxType.BASE, TEST_USER_ID, TEST_SESSION_ID);
            
        } catch (Exception e) {
            fail("Redis test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test 2: SandboxManager without Redis (in-memory mode)
     */
    @Test
    @Order(2)
    @DisplayName("Test SandboxManager without Redis (in-memory mode)")
    public void testSandboxManagerWithoutRedis() {
        ManagerConfig config = ManagerConfig.builder()
                .containerPrefixKey("inmemory_test_")
                .poolSize(0)
                .portRange(51000, 52000)
                .fileSystemConfig(LocalFileSystemConfig.builder()
                        .mountDir("sessions_mount_dir")
                        .build())
                .build();
        
        try (SandboxManager manager = new SandboxManager(config)) {
            assertNotNull(manager, "SandboxManager should be initialized");
            
            // Verify Redis is not enabled
            assertFalse(manager.getManagerConfig().getRedisEnabled(), 
                    "Redis should not be enabled");
            
            // Create a sandbox
            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE, 
                    "memory_user", 
                    "memory_session"
            );
            
            assertNotNull(container, "Container should be created");
            assertNotNull(container.getContainerName(), "Container should have a name");
            
            // Clean up
            manager.stopAndRemoveSandbox(SandboxType.BASE, "memory_user", "memory_session");
            
        } catch (Exception e) {
            fail("In-memory test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test 3: Shared state across multiple SandboxManager instances using Redis
     */
    @Test
    @Order(3)
    @DisplayName("Test shared state across multiple instances with Redis")
    public void testSharedStateWithRedis() {
        RedisManagerConfig redisConfig = RedisManagerConfig.builder()
                .redisServer("localhost")
                .redisPort(6379)
                .redisDb(0)
                .redisPortKey("_shared_test_ports")
                .redisContainerPoolKey("_shared_test_pool")
                .build();
        
        ManagerConfig config = ManagerConfig.builder()
                .containerPrefixKey("shared_test_")
                .redisConfig(redisConfig)
                .poolSize(0)
                .portRange(52000, 53000)
                .fileSystemConfig(LocalFileSystemConfig.builder()
                        .mountDir("sessions_mount_dir")
                        .build())
                .build();
        
        ContainerModel container1;
        String containerName1;
        
        // First manager instance creates a container
        try (SandboxManager manager1 = new SandboxManager(config)) {
            container1 = manager1.getSandbox(
                    SandboxType.BASE, 
                    "shared_user", 
                    "shared_session"
            );
            
            assertNotNull(container1, "Container should be created by manager1");
            containerName1 = container1.getContainerName();
            
            // Second manager instance accesses the same container
            try (SandboxManager manager2 = new SandboxManager(config)) {
                ContainerModel container2 = manager2.getSandbox(
                        SandboxType.BASE, 
                        "shared_user", 
                        "shared_session"
                );
                
                assertNotNull(container2, "Container should be accessible from manager2");
                assertEquals(containerName1, container2.getContainerName(), 
                        "Both managers should access the same container");
                
                // Clean up from manager 2
                manager2.stopAndRemoveSandbox(SandboxType.BASE, "shared_user", "shared_session");
            }
            
        } catch (Exception e) {
            fail("Shared state test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test 4: Container pool with Redis
     */
    @Test
    @Order(4)
    @DisplayName("Test container pool with Redis queue")
    public void testContainerPoolWithRedis() {
        RedisManagerConfig redisConfig = RedisManagerConfig.builder()
                .redisServer("localhost")
                .redisPort(6379)
                .redisDb(0)
                .redisPortKey("_pool_test_ports")
                .redisContainerPoolKey("_pool_test_queue")
                .build();
        
        ManagerConfig config = ManagerConfig.builder()
                .containerPrefixKey("pool_test_")
                .redisConfig(redisConfig)
                .poolSize(2)
                .portRange(53000, 54000)
                .fileSystemConfig(LocalFileSystemConfig.builder()
                        .mountDir("sessions_mount_dir")
                        .build())
                .build();
        
        try (SandboxManager manager = new SandboxManager(config)) {
            // Pool should be initialized with 2 containers
            assertNotNull(manager, "SandboxManager should be initialized");
            
            // Create container from pool
            ContainerModel container = manager.createFromPool(
                    SandboxType.BASE,
                    "pool_user",
                    "pool_session"
            );
            
            assertNotNull(container, "Container should be created from pool");
            assertNotNull(container.getContainerName(), "Container should have a name");
            
            // Clean up
            manager.release(container.getContainerName());
            
        } catch (Exception e) {
            fail("Container pool test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test 5: Container persistence in Redis
     */
    @Test
    @Order(5)
    @DisplayName("Test container data persistence in Redis")
    public void testContainerPersistenceInRedis() {
        RedisManagerConfig redisConfig = RedisManagerConfig.builder()
                .redisServer("localhost")
                .redisPort(6379)
                .redisDb(0)
                .redisPortKey("_persist_test_ports")
                .redisContainerPoolKey("_persist_test_pool")
                .build();
        
        ManagerConfig config = ManagerConfig.builder()
                .containerPrefixKey("persist_test_")
                .redisConfig(redisConfig)
                .poolSize(0)
                .portRange(54000, 55000)
                .fileSystemConfig(LocalFileSystemConfig.builder()
                        .mountDir("sessions_mount_dir")
                        .build())
                .build();
        
        String containerName;
        
        // Create container and verify it's stored in Redis
        try (SandboxManager manager = new SandboxManager(config)) {
            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE,
                    "persist_user",
                    "persist_session"
            );
            
            containerName = container.getContainerName();
            assertNotNull(containerName, "Container should have a name");
            
            // Container should be in getAllSandboxes (which reads from Redis)
            Map<SandboxKey, ContainerModel> sandboxes = manager.getAllSandboxes();
            assertFalse(sandboxes.isEmpty(), "Should have at least one sandbox in Redis");
            
            // Clean up
            manager.stopAndRemoveSandbox(SandboxType.BASE, "persist_user", "persist_session");
        }
    }
    
    @AfterEach
    public void afterEach() throws InterruptedException {
        // Wait a bit between tests to allow cleanup
        Thread.sleep(1000);
    }
}

