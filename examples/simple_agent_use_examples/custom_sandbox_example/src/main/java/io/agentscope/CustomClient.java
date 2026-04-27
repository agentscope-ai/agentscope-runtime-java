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
import io.agentscope.runtime.sandbox.manager.client.container.ContainerCreateResult;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CustomClient - A fake/stub implementation of {@link BaseClient} for teaching purposes.
 *
 * <h2>What is BaseClient?</h2>
 * <p>{@link BaseClient} is the core abstraction for managing "sandbox runtime instances".
 * A "runtime instance" could be anything:</p>
 * <ul>
 *   <li><b>A Docker container</b> — the most common case, managed via Docker API</li>
 *   <li><b>A Kubernetes Pod</b> — managed via K8s API</li>
 *   <li><b>An ECS instance / VM</b> — provisioned via cloud provider API (e.g., Alibaba Cloud ECS, AWS EC2)</li>
 *   <li><b>A serverless function</b> — invoked via Function Compute / AWS Lambda</li>
 *   <li><b>A remote bare-metal machine</b> — connected via SSH</li>
 *   <li><b>Your custom orchestration platform</b> — any system that can run sandbox images</li>
 * </ul>
 *
 * <h2>SandboxService Lifecycle</h2>
 * <p>The framework ({@code SandboxService}) calls these methods in a specific order:</p>
 * <pre>
 *   1. connect()          — Called once during SandboxService.start()
 *   2. imageExists()      — Check if the sandbox image/template is available
 *   3. pullImage()        — Download the image if not available (Docker-specific, can be no-op)
 *   4. createContainer()  — Provision a new runtime instance (container, VM, pod, etc.)
 *   5. startContainer()   — Start the provisioned instance
 *   6. getContainerStatus() — Poll instance health/readiness
 *      ... (sandbox runs, tools are called via HTTP to the instance) ...
 *   7. stopContainer()    — Stop the instance when sandbox.close() is called
 *   8. removeContainer()  — Clean up resources (delete VM, remove container, etc.)
 * </pre>
 *
 * <h2>How to adapt this to your platform</h2>
 * <p>Replace the fake/TODO implementations below with your platform's API calls.
 * Each method has comments explaining what different platforms would do.</p>
 *
 * @see BaseClient
 * @see CustomClientStarter
 */
public class CustomClient extends BaseClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomClient.class);

    private final CustomClientStarter config;
    private boolean connected = false;

    public CustomClient(CustomClientStarter config) {
        this.config = config;
    }

    // ===========================================================================
    // 1. CONNECTION MANAGEMENT
    //    Called once when SandboxService.start() initializes the client.
    // ===========================================================================

    /**
     * Establish connection to your runtime management platform.
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> Connect to Docker daemon via TCP or Unix socket</li>
     *   <li><b>Kubernetes:</b> Load kubeconfig and connect to K8s API server</li>
     *   <li><b>ECS/VM:</b> Initialize cloud SDK client with AccessKey, validate credentials</li>
     *   <li><b>SSH-based:</b> Establish SSH connection pool to remote machines</li>
     * </ul>
     *
     * @return true if connection is successful
     */
    @Override
    public boolean connect() {
        logger.info("[CustomClient] Connecting to platform at {}:{} ...", config.getHost(), config.getPort());

        // TODO: Replace with your platform's connection logic, for example:
        //   this.ecsClient = new EcsClient(config.getAccessKeyId(), config.getAccessKeySecret());
        //   this.ecsClient.describeRegions();  // validate credentials

        this.connected = true;
        logger.info("[CustomClient] Connected successfully (label={})", config.getLabel());
        return true;
    }

    /**
     * Check if the client is still connected to the platform.
     *
     * <p>Called periodically by the framework for health checks.</p>
     */
    @Override
    public boolean isConnected() {
        // TODO: Replace with actual health check, for example:
        //   return ecsClient.ping();
        return connected;
    }

    // ===========================================================================
    // 2. IMAGE / TEMPLATE MANAGEMENT
    //    Called before creating a container to ensure the sandbox image is ready.
    //    For non-Docker platforms, "image" could mean a VM template, AMI, etc.
    // ===========================================================================

    /**
     * Check if the sandbox image/template exists and is available.
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> Check if image exists locally via {@code docker images}</li>
     *   <li><b>Kubernetes:</b> Usually return true (K8s pulls images on pod creation)</li>
     *   <li><b>ECS/VM:</b> Check if the VM image/AMI/snapshot exists in the region</li>
     *   <li><b>Serverless:</b> Check if the function deployment package is ready</li>
     * </ul>
     *
     * @param imageName the sandbox image identifier (Docker image tag, AMI ID, etc.)
     * @return true if the image is available for use
     */
    @Override
    public boolean imageExists(String imageName) {
        logger.info("[CustomClient] Checking if image/template exists: {}", imageName);

        // TODO: Replace with your platform's image check logic, for example:
        //   DescribeImagesRequest req = new DescribeImagesRequest().setImageId(imageName);
        //   return !ecsClient.describeImages(req).getImages().isEmpty();

        // For this demo, we always return true (assume image is pre-deployed)
        return true;
    }

    /**
     * Download/prepare the sandbox image if it doesn't exist locally.
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> Pull image from registry ({@code docker pull})</li>
     *   <li><b>Kubernetes:</b> No-op (K8s handles image pulling during pod creation)</li>
     *   <li><b>ECS/VM:</b> Copy image from another region, or import from OSS/S3</li>
     *   <li><b>Serverless:</b> Deploy function code package</li>
     * </ul>
     *
     * @param imageName the sandbox image identifier
     * @return true if the image is now available
     */
    @Override
    public boolean pullImage(String imageName) {
        logger.info("[CustomClient] Pulling/preparing image: {}", imageName);

        // TODO: Replace with your platform's image preparation logic
        // For non-Docker platforms, this might be a no-op or trigger a different workflow

        return true;
    }

    // ===========================================================================
    // 3. INSTANCE LIFECYCLE MANAGEMENT
    //    These methods manage the full lifecycle of a sandbox runtime instance.
    //    The framework calls them in order: create → start → (use) → stop → remove
    // ===========================================================================

    /**
     * Create a new sandbox runtime instance.
     *
     * <p>This is the most important method. You need to:</p>
     * <ol>
     *   <li>Provision a new runtime instance (container, VM, pod, etc.)</li>
     *   <li>Configure networking (expose ports for the sandbox HTTP API)</li>
     *   <li>Set up storage (mount volumes/disks if needed)</li>
     *   <li>Inject environment variables</li>
     *   <li>Return the instance metadata in a {@link ContainerCreateResult}</li>
     * </ol>
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> {@code docker create} with port bindings and volume mounts</li>
     *   <li><b>Kubernetes:</b> Create a Pod spec with containers, services, and PVCs</li>
     *   <li><b>ECS/VM:</b> Call RunInstances API with security groups, VPC, user-data script</li>
     *   <li><b>Serverless:</b> Create/update function with the sandbox image</li>
     * </ul>
     *
     * <p><b>Key return value — {@link ContainerCreateResult}:</b></p>
     * <ul>
     *   <li>{@code containerId} — Unique instance identifier (container ID, instance ID, pod name)</li>
     *   <li>{@code ports} — List of exposed port numbers (the sandbox HTTP API port)</li>
     *   <li>{@code ip} — The IP/hostname where the sandbox API is accessible</li>
     * </ul>
     * <p>The framework uses {@code ip + ports} to build the sandbox HTTP endpoint
     * (e.g., {@code http://{ip}:{port}/fastapi/tools/run_ipython_cell}),
     * so it's critical that the sandbox service is reachable at this address.</p>
     *
     * @param containerName  unique name for the instance (generated by the framework)
     * @param imageName      the sandbox image to use
     * @param ports          ports that need to be exposed (e.g., ["80/tcp"])
     * @param volumeBindings host-to-container path mappings for file sharing
     * @param environment    environment variables to inject into the instance
     * @param runtimeConfig  additional runtime configuration (platform-specific)
     * @return metadata about the created instance
     */
    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName,
                                                  List<String> ports,
                                                  List<VolumeBinding> volumeBindings,
                                                  Map<String, String> environment,
                                                  Map<String, Object> runtimeConfig) {
        logger.info("[CustomClient] Creating runtime instance:");
        logger.info("  name       = {}", containerName);
        logger.info("  image      = {}", imageName);
        logger.info("  ports      = {}", ports);
        logger.info("  volumes    = {} binding(s)", volumeBindings != null ? volumeBindings.size() : 0);
        logger.info("  env vars   = {} variable(s)", environment != null ? environment.size() : 0);

        // TODO: Replace with your platform's instance creation logic, for example:
        //
        // --- Docker example ---
        //   CreateContainerResponse resp = dockerClient.createContainerCmd(imageName)
        //       .withName(containerName).withEnv(envList).exec();
        //   String instanceId = resp.getId();
        //
        // --- ECS example ---
        //   RunInstancesRequest req = new RunInstancesRequest()
        //       .setImageId(imageName)
        //       .setInstanceName(containerName)
        //       .setSecurityGroupId(config.getSecurityGroupId())
        //       .setVSwitchId(config.getVSwitchId());
        //   String instanceId = ecsClient.runInstances(req).getInstanceIdSets().get(0);
        //
        // --- Kubernetes example ---
        //   V1Pod pod = new V1PodBuilder().withNewMetadata().withName(containerName).endMetadata()
        //       .withNewSpec().addNewContainer().withImage(imageName).endContainer().endSpec().build();
        //   String instanceId = k8sClient.createNamespacedPod(namespace, pod).getMetadata().getUid();

        String instanceId = "fake-" + UUID.randomUUID().toString().substring(0, 12);

        // The port that the sandbox HTTP API listens on
        // For Docker: this is the mapped host port
        // For ECS/VM: this is the port on the instance's public/private IP
        List<String> exposedPorts = new ArrayList<>();
        exposedPorts.add("8080");  // fake port for demonstration

        // The IP/hostname where the instance is reachable
        // For Docker: usually "127.0.0.1" (localhost)
        // For ECS/VM: the instance's public IP or private IP within VPC
        // For K8s: the Service ClusterIP or NodePort
        String instanceIp = "127.0.0.1";

        logger.info("[CustomClient] Instance created: id={}, ip={}, ports={}", instanceId, instanceIp, exposedPorts);

        return new ContainerCreateResult(instanceId, exposedPorts, instanceIp);
    }

    /**
     * Start a previously created instance.
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> {@code docker start <containerId>}</li>
     *   <li><b>Kubernetes:</b> Usually no-op (pods start automatically after creation)</li>
     *   <li><b>ECS/VM:</b> Call StartInstance API, wait for "Running" status</li>
     *   <li><b>Serverless:</b> No-op (functions start on invocation)</li>
     * </ul>
     */
    @Override
    public void startContainer(String containerId) {
        logger.info("[CustomClient] Starting instance: {}", containerId);

        // TODO: Replace with your platform's start logic, for example:
        //   ecsClient.startInstance(new StartInstanceRequest().setInstanceId(containerId));
        //   waitForStatus(containerId, "Running", 60);
    }

    /**
     * Stop a running instance.
     *
     * <p>Called when {@code sandbox.close()} is invoked or try-with-resources exits.</p>
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> {@code docker stop <containerId>}</li>
     *   <li><b>Kubernetes:</b> Delete the Pod (K8s doesn't have a "stop" concept)</li>
     *   <li><b>ECS/VM:</b> Call StopInstance API</li>
     *   <li><b>Serverless:</b> No-op (functions stop after timeout)</li>
     * </ul>
     */
    @Override
    public void stopContainer(String containerId) {
        logger.info("[CustomClient] Stopping instance: {}", containerId);

        // TODO: Replace with your platform's stop logic
    }

    /**
     * Remove/destroy an instance and clean up all associated resources.
     *
     * <p>Called after stop to fully release resources (delete VM, remove container, etc.).</p>
     *
     * <p><b>What different platforms would do here:</b></p>
     * <ul>
     *   <li><b>Docker:</b> {@code docker rm <containerId>}</li>
     *   <li><b>Kubernetes:</b> Delete the Pod + associated Services/PVCs</li>
     *   <li><b>ECS/VM:</b> Call DeleteInstance API, release EIP, delete disks</li>
     *   <li><b>Serverless:</b> Delete the function instance</li>
     * </ul>
     */
    @Override
    public void removeContainer(String containerId) {
        logger.info("[CustomClient] Removing instance: {}", containerId);

        // TODO: Replace with your platform's cleanup logic
    }

    /**
     * Get the current status of an instance.
     *
     * <p>Used by the framework for health monitoring and cleanup scheduling.</p>
     *
     * @param containerId the instance identifier
     * @return status string (e.g., "running", "stopped", "terminated")
     */
    @Override
    public String getContainerStatus(String containerId) {
        // TODO: Replace with actual status query
        return "running";
    }

    // ===========================================================================
    // 4. INSTANCE INSPECTION
    //    Utility methods for checking if instances exist.
    // ===========================================================================

    /**
     * Check if an instance exists (regardless of its state).
     *
     * @param containerIdOrName instance identifier or name
     * @return true if the instance exists
     */
    @Override
    public boolean inspectContainer(String containerIdOrName) {
        // TODO: Replace with actual inspection logic
        return true;
    }

    /**
     * Check if an instance with the given name already exists.
     *
     * <p>Used by the framework to avoid name collisions when creating new instances.</p>
     */
    @Override
    public boolean containerNameExists(String containerName) {
        // TODO: Replace with actual name lookup
        return false;
    }
}
