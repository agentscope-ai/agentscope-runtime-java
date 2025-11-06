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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisSandboxManagerTest {

    private static final String TEST_USER_ID = "test_user";
    private static final String TEST_SESSION_ID = "test_session";

    @Test
    @Order(1)
    public void testSandboxManagerWithRedis() {
        RedisManagerConfig redisConfig = RedisManagerConfig.builder()
                .redisServer("localhost")
                .redisPort(6379)
                .redisDb(0)
                .redisPassword(null)
                .redisPortKey("_test_runtime_sandbox_occupied_ports")
                .redisContainerPoolKey("_test_runtime_sandbox_pool")
                .build();
        
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
            
            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE, 
                    TEST_USER_ID, 
                    TEST_SESSION_ID
            );
            
            assertNotNull(container, "Container should be created");
            assertNotNull(container.getContainerName(), "Container should have a name");
            assertNotNull(container.getBaseUrl(), "Container should have a base URL");
            assertTrue(container.getBaseUrl().contains("localhost"), "Base URL should contain localhost");
            
            Map<SandboxKey, ContainerModel> allSandboxes = manager.getAllSandboxes();
            assertEquals(1, allSandboxes.size(), "Should have 1 sandbox");
            
            manager.stopAndRemoveSandbox(SandboxType.BASE, TEST_USER_ID, TEST_SESSION_ID);
            
        } catch (Exception e) {
            fail("Redis test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
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
            
            assertFalse(manager.getManagerConfig().getRedisEnabled(),
                    "Redis should not be enabled");
            
            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE, 
                    "memory_user", 
                    "memory_session"
            );
            
            assertNotNull(container, "Container should be created");
            assertNotNull(container.getContainerName(), "Container should have a name");

            manager.stopAndRemoveSandbox(SandboxType.BASE, "memory_user", "memory_session");
            
        } catch (Exception e) {
            fail("In-memory test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
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
        
        try (SandboxManager manager1 = new SandboxManager(config)) {
            container1 = manager1.getSandbox(
                    SandboxType.BASE, 
                    "shared_user", 
                    "shared_session"
            );
            
            assertNotNull(container1, "Container should be created by manager1");
            containerName1 = container1.getContainerName();
            
            try (SandboxManager manager2 = new SandboxManager(config)) {
                ContainerModel container2 = manager2.getSandbox(
                        SandboxType.BASE, 
                        "shared_user", 
                        "shared_session"
                );
                
                assertNotNull(container2, "Container should be accessible from manager2");
                assertEquals(containerName1, container2.getContainerName(), 
                        "Both managers should access the same container");
                
                manager2.stopAndRemoveSandbox(SandboxType.BASE, "shared_user", "shared_session");
            }
            
        } catch (Exception e) {
            fail("Shared state test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
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
            assertNotNull(manager, "SandboxManager should be initialized");
            
            ContainerModel container = manager.createFromPool(
                    SandboxType.BASE,
                    "pool_user",
                    "pool_session"
            );
            
            assertNotNull(container, "Container should be created from pool");
            assertNotNull(container.getContainerName(), "Container should have a name");

            manager.release(container.getContainerName());
            
        } catch (Exception e) {
            fail("Container pool test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(5)
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
        
        try (SandboxManager manager = new SandboxManager(config)) {
            ContainerModel container = manager.getSandbox(
                    SandboxType.BASE,
                    "persist_user",
                    "persist_session"
            );
            
            containerName = container.getContainerName();
            assertNotNull(containerName, "Container should have a name");
            
            Map<SandboxKey, ContainerModel> sandboxes = manager.getAllSandboxes();
            assertFalse(sandboxes.isEmpty(), "Should have at least one sandbox in Redis");
            
            manager.stopAndRemoveSandbox(SandboxType.BASE, "persist_user", "persist_session");
        }
    }
    
    @AfterEach
    public void afterEach() throws InterruptedException {
        Thread.sleep(1000);
    }
}

