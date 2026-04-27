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

package io.agentscope;

import io.agentscope.runtime.sandbox.manager.client.container.BaseClient;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomClientStarter - A custom {@link BaseClientStarter} implementation that sits alongside
 * {@code DockerClientStarter}, {@code KubernetesClientStarter}, {@code AgentRunClientStarter},
 * and {@code FcClientStarter} as a pluggable sandbox runtime backend.
 *
 * <h2>What is BaseClientStarter?</h2>
 * <p>BaseClientStarter is a factory that holds connection configuration and creates a
 * {@link BaseClient} instance. It's the entry point for plugging in a custom runtime backend.</p>
 *
 * <h2>Existing implementations for reference</h2>
 * <pre>
 *   DockerClientStarter       → host, port, certPath
 *   KubernetesClientStarter   → kubeConfigPath, namespace
 *   AgentRunClientStarter     → accessKeyId, accessKeySecret, regionId, vpcId, cpu, memory, ...
 *   FcClientStarter           → accessKeyId, accessKeySecret, regionId, ...
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>
 *   // Plug your custom starter into ManagerConfig:
 *   ManagerConfig config = ManagerConfig.builder()
 *       .clientStarter(CustomClientStarter.builder()
 *           .host("my-platform.example.com")
 *           .port(443)
 *           .label("production")
 *           .build())
 *       .build();
 * </pre>
 *
 * <h2>How to adapt this to your platform</h2>
 * <p>Change the configuration fields to match your platform's needs. For example:</p>
 * <ul>
 *   <li><b>ECS:</b> accessKeyId, accessKeySecret, regionId, securityGroupId, vSwitchId</li>
 *   <li><b>SSH:</b> sshHost, sshPort, sshUser, privateKeyPath</li>
 *   <li><b>Custom API:</b> apiEndpoint, apiToken, maxRetries, timeout</li>
 * </ul>
 */
public class CustomClientStarter extends BaseClientStarter {

    private static final Logger logger = LoggerFactory.getLogger(CustomClientStarter.class);

    // --- Configuration fields ---
    // These are the parameters your platform needs to connect.
    // Change them to match your platform's requirements.
    private final String host;
    private final int port;
    private final String label;

    private CustomClientStarter(Builder builder) {
        // ContainerClientType determines how SandboxService handles certain operations:
        //   - DOCKER: pulls images locally, uses local volume mounts
        //   - KUBERNETES: skips image pull (K8s handles it), uses PVCs
        //   - AGENTRUN/FC: skips local volume mounts, uses absolute paths
        //
        // Choose the type that best matches your platform's behavior.
        // If your platform behaves like Docker (local execution), use DOCKER.
        // If it's cloud-based (no local volumes), consider AGENTRUN or FC.
        super(ContainerClientType.DOCKER);
        this.host = builder.host;
        this.port = builder.port;
        this.label = builder.label;
    }

    /**
     * Create and connect the {@link BaseClient}.
     *
     * <p>This method is called by {@code SandboxService.start()} during initialization.
     * It should create your client instance and establish the connection.</p>
     *
     * @param portManager provides port allocation (used by Docker for host port mapping; may be unused by other platforms)
     * @return a connected BaseClient instance
     */
    @Override
    public BaseClient startClient(PortManager portManager) {
        logger.info("[CustomClientStarter] Creating client (host={}, port={}, label={})",
                host, port, label);
        CustomClient client = new CustomClient(this);
        client.connect();
        logger.info("[CustomClientStarter] Client ready");
        return client;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getLabel() { return label; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String host = "localhost";
        private int port = 8080;
        private String label = "custom";

        private Builder() {}
        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public CustomClientStarter build() { return new CustomClientStarter(this); }
    }
}
