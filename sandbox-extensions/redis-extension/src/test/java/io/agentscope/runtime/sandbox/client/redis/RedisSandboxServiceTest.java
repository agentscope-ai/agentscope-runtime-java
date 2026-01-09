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
package io.agentscope.runtime.sandbox.client.redis;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.map.RedisManagerConfig;
import io.agentscope.runtime.sandbox.map.RedisSandboxMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@EnabledIfDockerAvailable
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
public class RedisSandboxServiceTest {

    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private static final String TEST_USER_ID = "test_user";
    private static final String TEST_SESSION_ID = "test_session";

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.1.2")).withExposedPorts(6379); // #gitleaks:allow

    /**
     * Helper method to create RedisManagerConfig using the testcontainer's Redis instance
     */
    private static RedisManagerConfig createRedisConfig(String portKey, String poolKey) {
        return RedisManagerConfig.builder().build();
    }

    @Test
    @Order(1)
    public void testSandboxServiceWithRedis() {
        RedisManagerConfig redisConfig = createRedisConfig("_test_runtime_sandbox_occupied_ports", "_test_runtime_sandbox_pool");
        ManagerConfig config = ManagerConfig.builder().containerPrefixKey("redis_test_").portRange(new PortRange(50000, 51000)).sandboxMap(new RedisSandboxMap(redisConfig)).build();
        try (SandboxService service = new SandboxService(config)) {
            service.start();
            Assertions.assertNotNull(service, "SandboxService should be initialized");
            Sandbox sandbox = new BaseSandbox(service, TEST_USER_ID, TEST_SESSION_ID);
            Assertions.assertNotNull(sandbox, "Container should be created");
            Map<String, ContainerModel> allSandboxes = service.getAllSandboxes();
            Assertions.assertEquals(1, allSandboxes.size(), "Should have 1 sandbox");
            service.stopAndRemoveSandbox(sandbox.getSandboxId());
        } catch (Exception e) {
            Assertions.fail("Redis test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    public void testSandboxServiceWithoutRedis() {
        ManagerConfig config = ManagerConfig.builder().containerPrefixKey("inmemory_test_").portRange(new PortRange(51000, 52000)).build();
        try (SandboxService service = new SandboxService(config)) {
            service.start();
            Assertions.assertNotNull(service, "SandboxService should be initialized");
            Sandbox sandbox = new BaseSandbox(service, "memory_user", "memory_session");
            Assertions.assertNotNull(sandbox, "Container should be created");
            Assertions.assertNotNull(sandbox.getSandboxId(), "Container should have a ID");
            service.stopAndRemoveSandbox(sandbox.getSandboxId());
        } catch (Exception e) {
            Assertions.fail("In-memory test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    public void testSharedStateWithRedis() {
        RedisManagerConfig redisConfig = createRedisConfig("_shared_test_ports", "_shared_test_pool");
        ManagerConfig config = ManagerConfig.builder().containerPrefixKey("shared_test_").sandboxMap(new RedisSandboxMap(redisConfig)).portRange(new PortRange(52000, 53000)).build();
        try (SandboxService service1 = new SandboxService(config)) {
            service1.start();
            Sandbox sandbox1 = new BaseSandbox(service1, TEST_USER_ID, TEST_SESSION_ID);
            Assertions.assertNotNull(sandbox1, "Container should be created by service1");
            String containerId1 = sandbox1.getSandboxId();
            try (SandboxService service2 = new SandboxService(config)) {
                service2.start();
                Sandbox sandbox2 = new BaseSandbox(service1, TEST_USER_ID, TEST_SESSION_ID);
                Assertions.assertNotNull(sandbox2, "Container should be accessible from service2");
                Assertions.assertEquals(containerId1, sandbox2.getSandboxId(), "Both services should access the same container");
                service2.stopAndRemoveSandbox(containerId1);
            }
        } catch (Exception e) {
            Assertions.fail("Shared state test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    public void testContainerPoolWithRedis() {
        RedisManagerConfig redisConfig = createRedisConfig("_pool_test_ports", "_pool_test_queue");
        ManagerConfig config = ManagerConfig.builder().containerPrefixKey("pool_test_").sandboxMap(new RedisSandboxMap(redisConfig)).portRange(new PortRange(53000, 54000)).build();
        try (SandboxService service = new SandboxService(config)) {
            service.start();
            Assertions.assertNotNull(service, "SandboxService should be initialized");
            Sandbox sandbox = new BaseSandbox(service, TEST_USER_ID, TEST_SESSION_ID);
            Assertions.assertNotNull(sandbox, "Container should be created from pool");
            Assertions.assertNotNull(sandbox.getSandboxId(), "Container should have a name");
            service.stopSandbox(sandbox.getSandboxId());
        } catch (Exception e) {
            Assertions.fail("Container pool test failed: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(5)
    public void testContainerPersistenceInRedis() {
        RedisManagerConfig redisConfig = createRedisConfig("_persist_test_ports", "_persist_test_pool");
        ManagerConfig config = ManagerConfig.builder().containerPrefixKey("persist_test_").sandboxMap(new RedisSandboxMap(redisConfig)).portRange(new PortRange(54000, 55000)).build();
        try (SandboxService service = new SandboxService(config)) {
            service.start();
            Sandbox sandbox = new BaseSandbox(service, TEST_USER_ID, TEST_SESSION_ID);
            Assertions.assertNotNull(sandbox.getSandboxId(), "Container should have a name");
            Map<String, ContainerModel> sandboxes = service.getAllSandboxes();
            Assertions.assertFalse(sandboxes.isEmpty(), "Should have at least one sandbox in Redis");
            service.stopAndRemoveSandbox(sandbox.getSandboxId());
        }
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        Thread.sleep(1000);
    }
}

