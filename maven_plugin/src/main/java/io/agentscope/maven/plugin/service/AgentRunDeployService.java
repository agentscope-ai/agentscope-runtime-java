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

package io.agentscope.maven.plugin.service;

import com.aliyun.agentrun20250910.Client;
import com.aliyun.agentrun20250910.models.CodeConfiguration;
import com.aliyun.agentrun20250910.models.CreateAgentRuntimeEndpointInput;
import com.aliyun.agentrun20250910.models.CreateAgentRuntimeEndpointRequest;
import com.aliyun.agentrun20250910.models.CreateAgentRuntimeEndpointResponse;
import com.aliyun.agentrun20250910.models.CreateAgentRuntimeInput;
import com.aliyun.agentrun20250910.models.CreateAgentRuntimeRequest;
import com.aliyun.agentrun20250910.models.CreateAgentRuntimeResponse;
import com.aliyun.agentrun20250910.models.GetAgentRuntimeRequest;
import com.aliyun.agentrun20250910.models.GetAgentRuntimeResponse;
import com.aliyun.agentrun20250910.models.LogConfiguration;
import com.aliyun.agentrun20250910.models.NetworkConfiguration;
import com.aliyun.agentrun20250910.models.UpdateAgentRuntimeInput;
import com.aliyun.agentrun20250910.models.UpdateAgentRuntimeRequest;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.SetBucketTaggingRequest;
import com.aliyun.oss.model.TagSet;
import io.agentscope.maven.plugin.config.AgentRunConfig;
import io.agentscope.maven.plugin.config.BuildConfig;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Java-side port of the Python AgentRun deployer:
 * <ol>
 *     <li>Ensure OSS bucket exists and is tagged for AgentRun access</li>
 *     <li>Upload artifact to OSS</li>
 *     <li>Create / update AgentRun runtime and default endpoint</li>
 * </ol>
 *
 * This assumes the Java SDK exposes APIs similar to the Python
 * {@code alibabacloud_agentrun20250910} client.
 */
public class AgentRunDeployService {

    private static final String DEFAULT_ENDPOINT_NAME = "default-endpoint";
    private static final int RUNTIME_STATUS_MAX_ATTEMPTS = 30;
    private static final long RUNTIME_STATUS_POLL_INTERVAL_MS = 10_000;
    private static final java.util.Set<String> RUNTIME_READY_STATUSES = java.util.Set.of("READY", "ACTIVE");
    private static final java.util.Set<String> RUNTIME_FAILED_STATUSES = java.util.Set.of(
            "CREATE_FAILED", "UPDATE_FAILED", "FAILED", "DELETING"
    );

    private final Log log;

    public AgentRunDeployService(Log log) {
        this.log = log;
    }

    public AgentRunDeployResult deploy(File artifact, BuildConfig buildConfig, AgentRunConfig config) throws Exception {
        if (artifact == null || !artifact.exists()) {
            throw new IOException("Artifact for AgentRun deployment is missing");
        }

        // Environment variables are already merged in DeployerMojo
        // Priority: agentrun.environment > environment (top-level) > build.environment
        Map<String, String> environment = config.getEnvironment();
        if (environment == null) {
            environment = new HashMap<>();
        }
        log.info("Environment variables: " + environment.keySet());

        File packagedArtifact = packageArtifact(artifact, environment);

        int containerPort = buildConfig != null ? buildConfig.getPort() : 8080;

        // Ensure the packaged artifact is cleaned up eventually
        packagedArtifact.deleteOnExit();

        // 1. Upload artifact to OSS
        OSS ossClient = createOssClient(config);
        String bucket = config.getArtifactBucket();
        String objectKey = ensureBucketAndUpload(ossClient, bucket, packagedArtifact);

        // 2. Create AgentRun client
        Client agentRunClient = createAgentRunClient(config);
        log.info("Created AgentRun client for region: " + config.getRegion());

        String agentRuntimeId = config.getExistingRuntimeId();
        String runtimeName = buildRuntimeName(config);

        if (agentRuntimeId == null || agentRuntimeId.isEmpty()) {
            // Create Agent Runtime
            CreateAgentRuntimeResponse createRuntimeResp = createAgentRuntime(
                    agentRunClient, config, runtimeName, bucket, objectKey, environment, containerPort);
            agentRuntimeId = extractAgentRuntimeId(createRuntimeResp);
            waitForRuntimeReady(agentRunClient, agentRuntimeId);
        } else {
            log.info("Updating existing AgentRun runtime: " + agentRuntimeId);
            updateAgentRuntime(agentRunClient, config, agentRuntimeId, bucket, objectKey, environment, containerPort);
            waitForRuntimeReady(agentRunClient, agentRuntimeId);
        }

        // 4. Create or reuse Default Endpoint
        CreateAgentRuntimeEndpointResponse endpointResp = createDefaultEndpoint(agentRunClient, agentRuntimeId, runtimeName);
        String endpointUrl = extractEndpointUrl(endpointResp);

        // 5. Build console URL (approximate)
        String consoleUrl = String.format(
                "https://functionai.console.aliyun.com/%s/agent/infra/agent-runtime/agent-detail?id=%s",
                config.getRegion(),
                agentRuntimeId
        );

        log.info("AgentRun deployment finished. RuntimeId=" + agentRuntimeId
                + ", consoleUrl=" + consoleUrl);

        return new AgentRunDeployResult(endpointUrl, agentRuntimeId, packagedArtifact);
    }

    private OSS createOssClient(AgentRunConfig config) {
        if (config.getAccessKeyId() == null || config.getAccessKeySecret() == null) {
            throw new IllegalStateException("AgentRun OSS accessKeyId/accessKeySecret must be configured");
        }
        String endpoint = String.format("https://oss-%s.aliyuncs.com", config.getRegion());
        if (config.getEndpoint() != null && !config.getEndpoint().isEmpty()) {
            endpoint = config.getEndpoint().replace("agentrun.", "oss-");
        }
        return new OSSClientBuilder().build(endpoint, config.getAccessKeyId(), config.getAccessKeySecret());
    }

    /**
     * Ensure bucket exists, apply AgentRun access tag, upload artifact, and return objectKey.
     */
    private String ensureBucketAndUpload(OSS ossClient, String bucket, File artifact) throws IOException {
        if (!ossClient.doesBucketExist(bucket)) {
            log.info("OSS bucket does not exist, creating: " + bucket);
            CreateBucketRequest request = new CreateBucketRequest(bucket);
            ossClient.createBucket(request);
        }
        SetBucketTaggingRequest taggingRequest = new SetBucketTaggingRequest(bucket);
        TagSet tagSet = new TagSet();
        tagSet.setTag("agentrun-deploy-access", "ReadAndAdd");
        taggingRequest.setTagSet(tagSet);
        ossClient.setBucketTagging(taggingRequest);

        String objectKey = artifact.getName();
        log.info("Uploading artifact to OSS. bucket=" + bucket + ", key=" + objectKey);
        try (FileInputStream fis = new FileInputStream(artifact)) {
            ossClient.putObject(bucket, objectKey, fis);
        } catch (Exception e) {
            throw new IOException("Failed to upload artifact to OSS: " + e.getMessage(), e);
        }

        Date expiration = Date.from(Instant.now().plusSeconds(3 * 60 * 60));
        String presignedUrl = ossClient.generatePresignedUrl(bucket, objectKey, expiration).toString();
        log.info("Generated AgentRun artifact presigned URL (3h): " + presignedUrl);
        return objectKey;
    }

    private Client createAgentRunClient(AgentRunConfig cfg) throws Exception {
        if (cfg.getAccessKeyId() == null || cfg.getAccessKeySecret() == null) {
            throw new IllegalStateException("AgentRun accessKeyId/accessKeySecret must be configured");
        }
        com.aliyun.teaopenapi.models.Config clientConfig = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(cfg.getAccessKeyId())
                .setAccessKeySecret(cfg.getAccessKeySecret())
                .setRegionId(cfg.getRegion());
        String endpoint = cfg.getEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = "agentrun." + cfg.getRegion() + ".aliyuncs.com";
        }
        clientConfig.setEndpoint(endpoint);
        return new Client(clientConfig);
    }

    private String buildRuntimeName(AgentRunConfig cfg) {
        String prefix = cfg.getRuntimeNamePrefix() != null && !cfg.getRuntimeNamePrefix().isEmpty()
                ? cfg.getRuntimeNamePrefix()
                : "agentscope-runtime";
        return prefix + "-" + UUID.randomUUID();
    }

    private CreateAgentRuntimeResponse createAgentRuntime(Client client,
                                                          AgentRunConfig cfg,
                                                          String runtimeName,
                                                          String bucket,
                                                          String objectKey,
                                                          Map<String, String> environment,
                                                          int port) throws Exception {
        CodeConfiguration codeConfig = new CodeConfiguration()
                .setLanguage(resolveCodeLanguage(cfg))
                .setCommand(Arrays.asList(new String[]{"java", "-jar", "app.jar"}))
                .setOssBucketName(bucket)
                .setOssObjectName(objectKey);

        CreateAgentRuntimeInput input = new CreateAgentRuntimeInput()
                .setAgentRuntimeName(runtimeName)
                .setArtifactType("Code")
                .setCpu((float) cfg.getCpu())
                .setMemory(cfg.getMemorySize())
                .setPort(port)
                .setCodeConfiguration(codeConfig)
                .setDescription("AgentScope auto-generated runtime for " + runtimeName)
                .setEnvironmentVariables(environment != null ? environment : new HashMap<>())
                .setExecutionRoleArn(cfg.getExecutionRoleArn())
                .setSessionConcurrencyLimitPerInstance(cfg.getSessionConcurrencyLimit())
                .setSessionIdleTimeoutSeconds(cfg.getSessionIdleTimeoutSeconds())
                .setLogConfiguration(buildLogConfiguration(cfg))
                .setNetworkConfiguration(buildNetworkConfiguration(cfg));

        CreateAgentRuntimeRequest request = new CreateAgentRuntimeRequest();
        request.setBody(input);

        return client.createAgentRuntime(request);
    }

    private void updateAgentRuntime(Client client,
                                    AgentRunConfig cfg,
                                    String runtimeId,
                                    String bucket,
                                    String objectKey,
                                    Map<String, String> environment,
                                    int port) throws Exception {
        CodeConfiguration codeConfig = new CodeConfiguration()
                .setLanguage(resolveCodeLanguage(cfg))
                .setCommand(Arrays.asList(new String[]{"java", "-jar", "/code/app.jar"}))
                .setOssBucketName(bucket)
                .setOssObjectName(objectKey);

        UpdateAgentRuntimeInput input = new UpdateAgentRuntimeInput()
                .setArtifactType("Code")
                .setCpu((float) cfg.getCpu())
                .setMemory(cfg.getMemorySize())
                .setPort(port)
                .setCodeConfiguration(codeConfig)
                .setEnvironmentVariables(environment != null ? environment : new HashMap<>())
                .setExecutionRoleArn(cfg.getExecutionRoleArn())
                .setSessionConcurrencyLimitPerInstance(cfg.getSessionConcurrencyLimit())
                .setSessionIdleTimeoutSeconds(cfg.getSessionIdleTimeoutSeconds())
                .setLogConfiguration(buildLogConfiguration(cfg))
                .setNetworkConfiguration(buildNetworkConfiguration(cfg));

        UpdateAgentRuntimeRequest request = new UpdateAgentRuntimeRequest();
        request.setBody(input);

        client.updateAgentRuntime(runtimeId, request);
    }

    private LogConfiguration buildLogConfiguration(AgentRunConfig cfg) {
        if (cfg.getLogProject() == null || cfg.getLogStore() == null) {
            return null;
        }
        return new LogConfiguration()
                .setProject(cfg.getLogProject())
                .setLogstore(cfg.getLogStore());
    }

    private NetworkConfiguration buildNetworkConfiguration(AgentRunConfig cfg) {
        boolean hasConfig = (cfg.getNetworkMode() != null && !cfg.getNetworkMode().isEmpty())
                || (cfg.getVpcId() != null && !cfg.getVpcId().isEmpty())
                || (cfg.getSecurityGroupId() != null && !cfg.getSecurityGroupId().isEmpty())
                || (cfg.getVswitchIds() != null && !cfg.getVswitchIds().isEmpty());
        if (!hasConfig) {
            return null;
        }
        NetworkConfiguration networkConfiguration = new NetworkConfiguration();
        networkConfiguration.setNetworkMode(cfg.getNetworkMode());
        networkConfiguration.setVpcId(cfg.getVpcId());
        networkConfiguration.setSecurityGroupId(cfg.getSecurityGroupId());
        if (cfg.getVswitchIds() != null && !cfg.getVswitchIds().isEmpty()) {
            networkConfiguration.setVswitchIds(cfg.getVswitchIds());
        }
        return networkConfiguration;
    }

    private void waitForRuntimeReady(Client client, String agentRuntimeId) throws Exception {
        String lastStatus = null;
        String lastReason = null;

        for (int attempt = 1; attempt <= RUNTIME_STATUS_MAX_ATTEMPTS; attempt++) {
            RuntimeStatus status = getRuntimeStatus(client, agentRuntimeId);
            lastStatus = status.status;
            lastReason = status.statusReason;

            log.info(String.format(
                    "AgentRun runtime status check %d/%d: %s%s",
                    attempt,
                    RUNTIME_STATUS_MAX_ATTEMPTS,
                    lastStatus,
                    lastReason != null ? " (" + lastReason + ")" : ""
            ));

            if (lastStatus != null) {
                if (RUNTIME_READY_STATUSES.contains(lastStatus)) {
                    return;
                }
                if (RUNTIME_FAILED_STATUSES.contains(lastStatus)) {
                    throw new IllegalStateException("AgentRun runtime " + agentRuntimeId
                            + " entered failure status '" + lastStatus + "'"
                            + (lastReason != null ? ": " + lastReason : ""));
                }
            }

            if (attempt < RUNTIME_STATUS_MAX_ATTEMPTS) {
                Thread.sleep(RUNTIME_STATUS_POLL_INTERVAL_MS);
            }
        }

        throw new IllegalStateException("Timed out waiting for AgentRun runtime " + agentRuntimeId
                + " to reach READY status. Last known status: " + lastStatus
                + (lastReason != null ? " (" + lastReason + ")" : ""));
    }

    private RuntimeStatus getRuntimeStatus(Client client, String agentRuntimeId) throws Exception {
        GetAgentRuntimeRequest request = new GetAgentRuntimeRequest();
        GetAgentRuntimeResponse response = client.getAgentRuntime(agentRuntimeId, request);
        RuntimeStatus status = new RuntimeStatus();

        if (response == null) {
            log.warn("Received null response when querying AgentRun runtime status.");
            return status;
        }

        Map<String, Object> responseMap = response.toMap();
        Object bodyObj = responseMap.get("body");
        if (bodyObj instanceof Map<?, ?> body) {
            Object dataObj = body.get("data");
            if (dataObj instanceof Map<?, ?> data) {
                Object runtimeStatus = data.get("status");
                Object statusReason = data.get("statusReason");
                if (runtimeStatus instanceof String) {
                    status.status = (String) runtimeStatus;
                }
                if (statusReason instanceof String) {
                    status.statusReason = (String) statusReason;
                }
            }
        }

        return status;
    }

    private static class RuntimeStatus {
        private String status;
        private String statusReason;
    }

    private String resolveCodeLanguage(AgentRunConfig cfg) {
        if (cfg.getCodeLanguage() == null || cfg.getCodeLanguage().isEmpty()) {
            return "java17";
        }
        return cfg.getCodeLanguage();
    }

    private File packageArtifact(File jar, Map<String, String> environment) throws IOException {
        Path tempDir = Files.createTempDirectory("agentrun-code");
        
        // Put files directly in tempDir (NOT in a "code" subdirectory)
        // AgentRun will extract zip contents to /code/, so:
        //   zip contains "app.jar" -> becomes "/code/app.jar"
        Path targetJar = tempDir.resolve("app.jar");
        Files.copy(jar.toPath(), targetJar, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied JAR to: " + targetJar);

        if (environment != null && !environment.isEmpty()) {
            Path envFile = tempDir.resolve(".env");
            StringBuilder builder = new StringBuilder();
            environment.forEach((k, v) -> {
                if (k != null && v != null) {
                    builder.append(k).append("=").append(v).append("\n");
                }
            });
            Files.write(envFile, builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("Created .env file with " + environment.size() + " variables");
        }

        Path zipPath = tempDir.resolve("agentrun-artifact.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
             Stream<Path> stream = Files.walk(tempDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> !path.equals(zipPath)) // exclude the zip file itself
                  .forEach(path -> {
                // Use filename relative to tempDir as entry name (no "code/" prefix)
                String entryName = tempDir.relativize(path).toString().replace("\\", "/");
                ZipEntry entry = new ZipEntry(entryName);
                try {
                    zos.putNextEntry(entry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                    log.debug("Added to zip: " + entryName);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to add file to zip: " + path, e);
                }
            });
        }
        
        log.info("Created artifact zip: " + zipPath + " (" + Files.size(zipPath) / 1024 + " KB)");
        return zipPath.toFile();
    }

    @SuppressWarnings("unchecked")
    private String extractAgentRuntimeId(CreateAgentRuntimeResponse resp) {
        if (resp == null) {
            throw new IllegalStateException("CreateAgentRuntimeResponse is null");
        }
        Map<String, Object> map = resp.toMap();
        Object bodyObj = map.get("body");
        if (bodyObj instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) bodyObj;
            Object dataObj = body.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object id = data.get("agentRuntimeId");
                if (id instanceof String && !((String) id).isEmpty()) {
                    return (String) id;
                }
            }
        }
        throw new IllegalStateException("Failed to extract agentRuntimeId from response");
    }

    private CreateAgentRuntimeEndpointResponse createDefaultEndpoint(Client client,
                                                                     String agentRuntimeId,
                                                                     String runtimeName) throws Exception {
        CreateAgentRuntimeEndpointInput input = new CreateAgentRuntimeEndpointInput()
                .setAgentRuntimeEndpointName(DEFAULT_ENDPOINT_NAME)
                .setTargetVersion("LATEST")
                .setDescription("AgentScope auto-generated endpoint for " + runtimeName);

        CreateAgentRuntimeEndpointRequest request = new CreateAgentRuntimeEndpointRequest();
        request.setBody(input);

        return client.createAgentRuntimeEndpoint(agentRuntimeId, request);
    }

    @SuppressWarnings("unchecked")
    private String extractEndpointUrl(CreateAgentRuntimeEndpointResponse resp) {
        if (resp == null) {
            return "";
        }
        Map<String, Object> map = resp.toMap();
        Object bodyObj = map.get("body");
        if (bodyObj instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) bodyObj;
            Object dataObj = body.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object url = data.get("agentRuntimePublicEndpointUrl");
                if (url instanceof String) {
                    return (String) url;
                }
            }
        }
        return "";
    }

    public static class AgentRunDeployResult {
        private final String endpoint;
        private final String deploymentId;
        private final File uploadedArtifact;

        public AgentRunDeployResult(String endpoint, String deploymentId, File uploadedArtifact) {
            this.endpoint = endpoint;
            this.deploymentId = deploymentId;
            this.uploadedArtifact = uploadedArtifact;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getDeploymentId() {
            return deploymentId;
        }

        public File getUploadedArtifact() {
            return uploadedArtifact;
        }
    }
}

