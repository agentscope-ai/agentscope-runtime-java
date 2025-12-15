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
package io.agentscope.runtime.sandbox.manager.client;

import com.aliyun.agentrun20250910.Client;
import com.aliyun.agentrun20250910.models.*;
import com.aliyun.teaopenapi.models.Config;
import io.agentscope.runtime.sandbox.manager.client.config.AgentRunClientConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Client for managing AgentRun containers in the sandbox environment.
 * <p>
 * This client provides methods to create, start, stop, remove,
 * and inspect AgentRun sessions. It also handles the underlying AgentRun
 * API calls and status polling.
 */
public class AgentRunClient extends BaseClient {
    private static final Logger logger = Logger.getLogger(AgentRunClient.class.getName());

    private static final int GET_AGENT_RUNTIME_STATUS_MAX_ATTEMPTS = 60;
    private static final int GET_AGENT_RUNTIME_STATUS_INTERVAL = 1; // seconds

    @SuppressWarnings("unused")
    private static final String DEFAULT_ENDPOINT_NAME = "default-endpoint";
    @SuppressWarnings("unused")
    private static final String HTTPS_PROTOCOL = "https";
    @SuppressWarnings("unused")
    private static final int HTTPS_PORT = 443;

    private AgentRunClientConfig config;
    private Client client;
    private AgentRunSessionManager sessionManager;
    private String agentRunPrefix;
    private int getAgentRuntimeStatusMaxAttempts;
    private int getAgentRuntimeStatusInterval;
    private boolean connected = false;

    public AgentRunClient(AgentRunClientConfig config) throws Exception {
        this.config = config;
        this.client = createAgentRunClient();
        this.sessionManager = new AgentRunSessionManager();
        this.agentRunPrefix = (config.getAgentRunPrefix() != null && !config.getAgentRunPrefix().isEmpty())
                ? config.getAgentRunPrefix() : "agentscope-sandbox";
        this.getAgentRuntimeStatusMaxAttempts = GET_AGENT_RUNTIME_STATUS_MAX_ATTEMPTS;
        this.getAgentRuntimeStatusInterval = GET_AGENT_RUNTIME_STATUS_INTERVAL;
        logger.info("AgentRunClient initialized with config: " + config
                + ", region: " + config.getAgentRunRegionId()
                + ", prefix: " + this.agentRunPrefix);
    }

    @Override
    public boolean connect() {
        try {
            this.connected = (client != null);
            return this.connected;
        } catch (Exception e) {
            logger.severe("Failed to connect to AgentRun: " + e.getMessage());
            this.connected = false;
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected && client != null;
    }

    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName, List<String> ports,
                                  List<VolumeBinding> volumeBindings,
                                  Map<String, String> environment, Map<String, Object> runtimeConfig) {
        logger.fine("Creating AgentRun session with image: " + imageName);

        @SuppressWarnings("unused")
        int port = 80;
        if (ports != null && !ports.isEmpty()) {
            String firstPort = ports.get(0);
            port = "80/tcp".equals(firstPort) ? 80 : parsePort(firstPort);
        }

        @SuppressWarnings("unused")
        String agentRunImage = replaceAgentRuntimeImages(imageName);

        try {
            String sessionId = (containerName != null && !containerName.isEmpty())
                    ? containerName : generateSessionId();
            logger.info("Created AgentRun session: " + sessionId);
            String agentRuntimeName = agentRunPrefix + "-" + sessionId;

            ContainerConfiguration containerConfig = new ContainerConfiguration();
            containerConfig.setImage(agentRunImage);

            NetworkConfiguration networkConfig = new NetworkConfiguration();
            if (config.getAgentRunVpcId() != null && config.getAgentRunSecurityGroupId() != null
                    && config.getAgentRunVswitchIds() != null && !config.getAgentRunVswitchIds().isEmpty()) {
                logger.info("Create agent runtime with PUBLIC_AND_PRIVATE network");
                networkConfig.setNetworkMode("PUBLIC_AND_PRIVATE");
                networkConfig.setVpcId(config.getAgentRunVpcId());
                networkConfig.setSecurityGroupId(config.getAgentRunSecurityGroupId());
                networkConfig.setVswitchIds(config.getAgentRunVswitchIds());
            } else {
                logger.info("Create agent runtime with PUBLIC network");
                networkConfig.setNetworkMode("PUBLIC");
            }

            LogConfiguration logConfig = null;
            if (config.getAgentrunLogProject() != null && config.getAgentrunLogStore() != null) {
                logConfig = new LogConfiguration();
                logConfig.setProject(config.getAgentrunLogProject());
                logConfig.setLogstore(config.getAgentrunLogStore());
            }

            HealthCheckConfiguration healthCheckConfig = new HealthCheckConfiguration();
            healthCheckConfig.setHttpGetUrl("/");
            healthCheckConfig.setInitialDelaySeconds(2);
            healthCheckConfig.setPeriodSeconds(1);
            healthCheckConfig.setSuccessThreshold(1);
            healthCheckConfig.setFailureThreshold(60);
            healthCheckConfig.setTimeoutSeconds(1);

            CreateAgentRuntimeInput inputData = new CreateAgentRuntimeInput();
            inputData.setAgentRuntimeName(agentRuntimeName);
            inputData.setArtifactType("Container");
            inputData.setCpu(config.getAgentRunCpu());
            inputData.setMemory(config.getAgentRunMemory());
            inputData.setPort(port);
            inputData.setContainerConfiguration(containerConfig);
            inputData.setEnvironmentVariables(environment != null ? environment : new HashMap<>());
            inputData.setNetworkConfiguration(networkConfig);
            inputData.setLogConfiguration(logConfig);
            inputData.setHealthCheckConfiguration(healthCheckConfig);
            inputData.setDescription("agentScope sandbox deploy for " + agentRuntimeName);

            String agentRuntimeId = createAndCheckAgentRuntime(inputData);

            String[] endpointResult = createAndCheckAgentRuntimeEndpoint(agentRuntimeId, agentRuntimeName);
            String agentRuntimeEndpointId = endpointResult[0];
            String endpointPublicUrl = endpointResult[1];

            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("session_id", sessionId);
            sessionData.put("created_time", System.currentTimeMillis() / 1000.0);
            sessionData.put("agent_runtime_name", agentRuntimeName);
            sessionData.put("agent_runtime_id", agentRuntimeId);
            sessionData.put("agent_runtime_endpoint_id", agentRuntimeEndpointId);
            sessionData.put("endpoint_public_url", endpointPublicUrl);
            sessionData.put("status", "running");
            sessionData.put("image", imageName);

            if (environment != null && environment.containsKey("SECRET_TOKEN")) {
                sessionData.put("runtime_token", environment.get("SECRET_TOKEN"));
            }
            sessionData.put("environment", environment);

            String endpointPublicUrlDomain = null;
            String endpointPublicUrlPath = null;
            List<String> agentRunPorts = new ArrayList<>();
            
            if (!endpointPublicUrl.endsWith("/")) {
                endpointPublicUrl += '/';
            }

            try {
                URI parsedUrl = new URI(endpointPublicUrl);
                String host = parsedUrl.getHost();
                int tmpPort = parsedUrl.getPort();
                endpointPublicUrlDomain = host;
                if (tmpPort == -1) {
                    tmpPort = HTTPS_PORT;
                }
                endpointPublicUrlPath = parsedUrl.getPath();
                if (endpointPublicUrlPath == null || endpointPublicUrlPath.isEmpty()) {
                    endpointPublicUrlPath = "/";
                }
                agentRunPorts.add(tmpPort + endpointPublicUrlPath);
                sessionData.put("endpoint_domain", endpointPublicUrlDomain);
                sessionData.put("endpoint_path", endpointPublicUrlPath);
                sessionData.put("ports", agentRunPorts);
                
                logger.info("Domain (netloc): " + endpointPublicUrlDomain);
                logger.info("Path: " + endpointPublicUrlPath);
                logger.info("Ports entry: " + agentRunPorts.get(0));
            } catch (URISyntaxException e) {
                logger.warning("Failed to parse endpoint URL: " + endpointPublicUrl + ", error: " + e.getMessage());
                agentRunPorts.add(String.valueOf(HTTPS_PORT));
                sessionData.put("ports", agentRunPorts);
            }

            sessionManager.createSession(sessionId, sessionData);
            logger.info("Success to create agent runtime with ID: " + agentRuntimeId + ", create session id: " + sessionId);
            logger.info("endpoint_public_url: " + endpointPublicUrl);

            return new ContainerCreateResult(sessionId, agentRunPorts, endpointPublicUrlDomain, HTTPS_PROTOCOL);

        } catch (Exception e) {
            logger.severe("Failed to create AgentRun session: " + e.getMessage());
            throw new RuntimeException("Failed to create AgentRun session", e);
        }
    }

    @Override
    public void startContainer(String containerId) {
        Map<String, Object> session = sessionManager.getSession(containerId);
        if (session == null) {
            logger.warning("AgentRun session id not found: " + containerId);
            return;
        }
        String agentRuntimeId = (String) session.get("agent_runtime_id");
        try {
            Map<String, Object> resp = getAgentRuntimeStatus(agentRuntimeId);
            if (Boolean.TRUE.equals(resp.get("success"))) {
                String status = (String) resp.get("status");
                if ("READY".equals(status)) {
                    sessionManager.updateSession(containerId, Collections.singletonMap("status", "running"));
                }
            }
            logger.info("set agentRun session status to running: " + containerId);
        } catch (Exception e) {
            logger.severe("failed to set agentRun session status to running: " + containerId + ": " + e.getMessage());
        }
    }

    @Override
    public void stopContainer(String containerId) {
        Map<String, Object> session = sessionManager.getSession(containerId);
        if (session == null) {
            logger.warning("AgentRun session id not found: " + containerId);
            return;
        }

        try {
            sessionManager.updateSession(containerId, Collections.singletonMap("status", "stopped"));
            logger.info("set agentRun session status to stopped: " + containerId);
        } catch (Exception e) {
            logger.severe("failed to set agentRun session status to stopped: " + containerId + ": " + e.getMessage());
        }
    }

    @Override
    public void removeContainer(String containerId) {
        Map<String, Object> session = sessionManager.getSession(containerId);
        if (session == null) {
            logger.warning("AgentRun session id not found: " + containerId);
            return;
        }
        String agentRuntimeId = (String) session.get("agent_runtime_id");
        try {
            logger.info("Deleting agent runtime with ID: " + agentRuntimeId);

            DeleteAgentRuntimeResponse response = client.deleteAgentRuntime(agentRuntimeId);

            if (response.getBody() != null && "SUCCESS".equals(response.getBody().getCode())) {
                logger.info("Agent runtime deletion initiated successfully for ID: " + agentRuntimeId);

                Map<String, Object> pollStatus = pollAgentRuntimeStatus(agentRuntimeId);
                String statusResult = (String) pollStatus.get("status");
                logger.info("Agent runtime deletion status: " + statusResult);

                sessionManager.deleteSession(containerId);
                logger.info("Successfully removed AgentRun session: " + containerId);
            } else {
                logger.severe("Failed to delete agent runtime");
            }
        } catch (Exception e) {
            logger.severe("Exception occurred while deleting agent runtime: " + e.getMessage());
        }
    }

    @Override
    public String getContainerStatus(String containerId) {
        Map<String, Object> session = sessionManager.getSession(containerId);
        if (session == null) {
            logger.warning("AgentRun session id not found: " + containerId);
            return "unknown";
        }
        String agentRuntimeId = (String) session.get("agent_runtime_id");
        Map<String, Object> resp = getAgentRuntimeStatus(agentRuntimeId);
        if (Boolean.TRUE.equals(resp.get("success"))) {
            String agentRunStatus = (String) resp.get("status");
            if ("READY".equals(agentRunStatus) || "ACTIVE".equals(agentRunStatus)) {
                return "running";
            }
            if (Arrays.asList("CREATE_FAILED", "UPDATE_FAILED", "FAILED", "DELETING").contains(agentRunStatus)) {
                return "exited";
            }
            if (Arrays.asList("CREATING", "UPDATING").contains(agentRunStatus)) {
                return "starting";
            }
        }
        return (String) session.getOrDefault("status", "unknown");
    }

    @Override
    public boolean imageExists(String imageName) {
        return imageName != null && !imageName.isEmpty();
    }

    @Override
    public boolean inspectContainer(String containerIdOrName) {
        Map<String, Object> container = inspect(containerIdOrName);
        return container != null && !container.isEmpty();
    }

    @Override
    public boolean pullImage(String imageName) {
        logger.info("AgentRun automatically handles image pulling during container creation");
        return true;
    }

    /**
     * Inspect an AgentRun session and return detailed information.
     *
     * @param sessionId The ID of the session to inspect.
     * @return A map containing session information, agent runtime info, and status.
     */
    public Map<String, Object> inspect(String sessionId) {
        Map<String, Object> session = sessionManager.getSession(sessionId);
        if (session == null) {
            logger.warning("AgentRun session id not found: " + sessionId);
            return Collections.emptyMap();
        }
        String agentRuntimeId = (String) session.get("agent_runtime_id");

        Map<String, Object> agentRuntimeInfo = new HashMap<>();
        try {
            if (agentRuntimeId != null) {
                 GetAgentRuntimeRequest request = new GetAgentRuntimeRequest();
                 GetAgentRuntimeResponse response = client.getAgentRuntime(agentRuntimeId, request);

                 if (response.getBody() != null && "SUCCESS".equals(response.getBody().getCode())
                     && response.getBody().getData() != null) {
                     AgentRuntime data = response.getBody().getData();
                     agentRuntimeInfo.put("agent_runtime_id", data.getAgentRuntimeId());
                     agentRuntimeInfo.put("agent_runtime_name", data.getAgentRuntimeName());
                     agentRuntimeInfo.put("agent_runtime_arn", data.getAgentRuntimeArn());
                     agentRuntimeInfo.put("status", data.getStatus());
                     agentRuntimeInfo.put("status_reason", data.getStatusReason());
                     agentRuntimeInfo.put("artifact_type", data.getArtifactType());
                     agentRuntimeInfo.put("cpu", data.getCpu());
                     agentRuntimeInfo.put("memory", data.getMemory());
                     agentRuntimeInfo.put("port", data.getPort());
                     agentRuntimeInfo.put("created_at", data.getCreatedAt());
                     agentRuntimeInfo.put("last_updated_at", data.getLastUpdatedAt());
                     agentRuntimeInfo.put("description", data.getDescription());
                     agentRuntimeInfo.put("agent_runtime_version", data.getAgentRuntimeVersion());
                     agentRuntimeInfo.put("environment_variables", data.getEnvironmentVariables());
                     agentRuntimeInfo.put("request_id", response.getBody().getRequestId());

                 }
                 else{
                     logger.warning("Failed to get agent runtime info for ID: " + agentRuntimeId);
                     agentRuntimeInfo.put("error", "Failed to get agent runtime info");
                     agentRuntimeInfo.put("agent_runtime_id", agentRuntimeId);
                     agentRuntimeInfo.put("code", response.getBody() != null ? response.getBody().getCode() : null);
                     agentRuntimeInfo.put("message", response.getBody() != null 
                         ? "Failed to get agent runtime info" : null);
                 }
            }
        } catch (Exception e) {
            logger.severe("Exception occurred while getting agent runtime info: " + e.getMessage());
            agentRuntimeInfo.put("error", e.getMessage());
            agentRuntimeInfo.put("message", "Exception occurred while getting agent runtime info: " + e.getMessage());
            agentRuntimeInfo.put("agent_runtime_id", agentRuntimeId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        result.put("agent_runtime_info", agentRuntimeInfo);
        result.put("runtime_token", session.get("runtime_token"));
        result.put("status", session.getOrDefault("status", "unknown"));
        result.put("endpoint_url", session.get("endpoint_public_url"));
        return result;
    }

    private Client createAgentRunClient() throws Exception {
        Config agentRunConfig = new Config();
        agentRunConfig.setAccessKeyId(this.config.getAgentRunAccessKeyId());
        agentRunConfig.setAccessKeySecret(this.config.getAgentRunAccessKeySecret());
        agentRunConfig.setRegionId(this.config.getAgentRunRegionId());
        agentRunConfig.setReadTimeout(60 * 1000);
        agentRunConfig.setEndpoint("agentrun." + this.config.getAgentRunRegionId() + ".aliyuncs.com");
        return new Client(agentRunConfig);
    }

    private String generateSessionId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    private String createAndCheckAgentRuntime(CreateAgentRuntimeInput inputData) throws Exception {
        CreateAgentRuntimeRequest request = new CreateAgentRuntimeRequest();
        request.setBody(inputData);
        CreateAgentRuntimeResponse response = client.createAgentRuntime(request);

        String agentRuntimeId = null;
        if (response.getBody() != null && response.getBody().getData() != null) {
            agentRuntimeId = response.getBody().getData().getAgentRuntimeId();
        }

        if (agentRuntimeId != null) {
            Map<String, Object> statusResponse = pollAgentRuntimeStatus(agentRuntimeId);
            if (!Boolean.TRUE.equals(statusResponse.get("success"))) {
                throw new RuntimeException("Failed to get agent runtime status: " + statusResponse.get("message"));
            }
            String status = (String) statusResponse.get("status");
            if (!Arrays.asList("READY", "ACTIVE").contains(status)) {
                throw new RuntimeException("Agent runtime is not ready. Status: " + status);
            }
        }

        return agentRuntimeId;
    }

    @SuppressWarnings("unused")
    private String[] createAndCheckAgentRuntimeEndpoint(String agentRuntimeId, String agentRuntimeName) throws Exception {
        CreateAgentRuntimeEndpointInput endpointInput = new CreateAgentRuntimeEndpointInput();
        endpointInput.setAgentRuntimeEndpointName(DEFAULT_ENDPOINT_NAME);
        endpointInput.setTargetVersion("LATEST");
        endpointInput.setDescription("agentScope deploy auto-generated endpoint for " + agentRuntimeName);

        CreateAgentRuntimeEndpointRequest endpointRequest = new CreateAgentRuntimeEndpointRequest();
        endpointRequest.setBody(endpointInput);

        CreateAgentRuntimeEndpointResponse endpointResponse = client.createAgentRuntimeEndpoint(
                agentRuntimeId, endpointRequest);

        String agentRuntimeEndpointId = null;
        String endpointPublicUrl = null;
        if (endpointResponse.getBody() != null && endpointResponse.getBody().getData() != null) {
            agentRuntimeEndpointId = endpointResponse.getBody().getData().getAgentRuntimeEndpointId();
            endpointPublicUrl = endpointResponse.getBody().getData().getEndpointPublicUrl();
        }

        if (agentRuntimeId != null && agentRuntimeEndpointId != null) {
            Map<String, Object> endpointStatusResponse = pollAgentRuntimeEndpointStatus(
                    agentRuntimeId, agentRuntimeEndpointId);
            if (!Boolean.TRUE.equals(endpointStatusResponse.get("success"))) {
                throw new RuntimeException("Failed to get agent runtime endpoint status: "
                        + endpointStatusResponse.get("message"));
            }
            String status = (String) endpointStatusResponse.get("status");
            if (!Arrays.asList("READY", "ACTIVE").contains(status)) {
                throw new RuntimeException("Agent runtime endpoint is not ready. Status: " + status);
            }
        }

        return new String[]{agentRuntimeEndpointId, endpointPublicUrl};
    }

    @SuppressWarnings("unused")
    private Map<String, Object> pollAgentRuntimeEndpointStatus(String agentRuntimeId, String agentRuntimeEndpointId) {
        Set<String> terminalStates = new HashSet<>(Arrays.asList(
                "CREATE_FAILED", "UPDATE_FAILED", "READY", "ACTIVE", "FAILED", "DELETING"));

        int maxAttempts = getAgentRuntimeStatusMaxAttempts;
        int intervalSeconds = getAgentRuntimeStatusInterval;

        logger.info("Starting to poll agent runtime endpoint status for ID: " + agentRuntimeEndpointId);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> statusResponse = getAgentRuntimeEndpointStatus(agentRuntimeId, agentRuntimeEndpointId);

            if (!Boolean.TRUE.equals(statusResponse.get("success"))) {
                logger.warning("Attempt " + attempt + "/" + maxAttempts + ": Failed to get status - "
                        + statusResponse.get("message"));
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(intervalSeconds * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                continue;
            }

            String currentStatus = (String) statusResponse.get("status");
            String statusReason = (String) statusResponse.get("status_reason");

            logger.info("Attempt " + attempt + "/" + maxAttempts + ": Status = " + currentStatus);
            if (statusReason != null) {
                logger.info("  Status reason: " + statusReason);
            }

            if (terminalStates.contains(currentStatus)) {
                logger.info("Reached terminal state '" + currentStatus + "' after " + attempt + " attempts");
                return statusResponse;
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.warning("Exceeded maximum attempts (" + maxAttempts + ") without reaching a terminal state");
        return getAgentRuntimeEndpointStatus(agentRuntimeId, agentRuntimeEndpointId);
    }

    private Map<String, Object> getAgentRuntimeStatus(String agentRuntimeId) {
        return getAgentRuntimeStatus(agentRuntimeId, null);
    }

    private Map<String, Object> getAgentRuntimeStatus(String agentRuntimeId, String agentRuntimeVersion) {
        try {
            logger.fine("Getting agent runtime status for ID: " + agentRuntimeId);

            GetAgentRuntimeRequest request = new GetAgentRuntimeRequest();
            if (agentRuntimeVersion != null) {
                request.setAgentRuntimeVersion(agentRuntimeVersion);
            }
            GetAgentRuntimeResponse response = client.getAgentRuntime(agentRuntimeId, request);

            if (response.getBody() != null && "SUCCESS".equals(response.getBody().getCode())
                    && response.getBody().getData() != null) {
                String status = response.getBody().getData().getStatus();
                logger.fine("Agent runtime status for ID " + agentRuntimeId + ": " + status);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("status", status);
                result.put("status_reason", response.getBody().getData().getStatusReason());
                result.put("request_id", response.getBody().getRequestId());
                return result;
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", response.getBody() != null ? response.getBody().getCode() : null);
                result.put("message", "Failed to get agent runtime status");
                result.put("request_id", response.getBody() != null ? response.getBody().getRequestId() : null);
                return result;
            }

        } catch (Exception e) {
            logger.severe("Exception occurred while getting agent runtime status: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Exception occurred while getting agent runtime status: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> getAgentRuntimeEndpointStatus(String agentRuntimeId, String agentRuntimeEndpointId) {
        try {
            logger.fine("Getting agent runtime endpoint status for ID: " + agentRuntimeEndpointId);

            GetAgentRuntimeEndpointResponse response = client.getAgentRuntimeEndpoint(
                    agentRuntimeId, agentRuntimeEndpointId);

            if (response.getBody() != null && "SUCCESS".equals(response.getBody().getCode())
                    && response.getBody().getData() != null) {
                String status = response.getBody().getData().getStatus();
                logger.fine("Agent runtime endpoint status for ID " + agentRuntimeEndpointId + ": " + status);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("status", status);
                result.put("status_reason", response.getBody().getData().getStatusReason());
                result.put("request_id", response.getBody().getRequestId());
                return result;
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", response.getBody() != null ? response.getBody().getCode() : null);
                result.put("message", "Failed to get agent runtime endpoint status");
                result.put("request_id", response.getBody() != null ? response.getBody().getRequestId() : null);
                return result;
            }

        } catch (Exception e) {
            logger.fine("Exception occurred while getting agent runtime endpoint status: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Exception occurred while getting agent runtime endpoint status: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> pollAgentRuntimeStatus(String agentRuntimeId) {
        return pollAgentRuntimeStatus(agentRuntimeId, null);
    }

    private Map<String, Object> pollAgentRuntimeStatus(String agentRuntimeId, String agentRuntimeVersion) {
        Set<String> terminalStates = new HashSet<>(Arrays.asList(
                "CREATE_FAILED", "UPDATE_FAILED", "READY", "ACTIVE", "FAILED", "DELETING"));

        int maxAttempts = getAgentRuntimeStatusMaxAttempts;
        int intervalSeconds = getAgentRuntimeStatusInterval;

        logger.info("Starting to poll agent runtime status for ID: " + agentRuntimeId);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> statusResponse = getAgentRuntimeStatus(agentRuntimeId, agentRuntimeVersion);

            if (!Boolean.TRUE.equals(statusResponse.get("success"))) {
                logger.warning("Attempt " + attempt + "/" + maxAttempts + ": Failed to get status - "
                        + statusResponse.get("message"));
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(intervalSeconds * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                continue;
            }

            String currentStatus = (String) statusResponse.get("status");
            String statusReason = (String) statusResponse.get("status_reason");

            logger.info("Attempt " + attempt + "/" + maxAttempts + ": Status = " + currentStatus);
            if (statusReason != null) {
                logger.info("  Status reason: " + statusReason);
            }

            if (terminalStates.contains(currentStatus)) {
                logger.info("Reached terminal state '" + currentStatus + "' after " + attempt + " attempts");
                return statusResponse;
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.warning("Exceeded maximum attempts (" + maxAttempts + ") without reaching a terminal state");
        return getAgentRuntimeStatus(agentRuntimeId, agentRuntimeVersion);
    }

    private String replaceAgentRuntimeImages(String image) {
        Map<String, String> replacementMap = new HashMap<>();
        replacementMap.put("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base",
                "serverless-registry.cn-hangzhou.cr.aliyuncs.com/functionai/agentscope_runtime-sandbox-base:20251027");
        replacementMap.put("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser",
                "serverless-registry.cn-hangzhou.cr.aliyuncs.com/functionai/agentscope_runtime-sandbox-browser:20251027");
        replacementMap.put("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem",
                "serverless-registry.cn-hangzhou.cr.aliyuncs.com/functionai/agentscope_runtime-sandbox-filesystem:20251027");
        replacementMap.put("agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui",
                "serverless-registry.cn-hangzhou.cr.aliyuncs.com/functionai/agentscope_runtime-sandbox-gui:20251027");

        String imageName;
        if (image.contains(":")) {
            imageName = image.split(":", 2)[0];
        } else {
            imageName = image;
        }

        return replacementMap.getOrDefault(imageName.trim(), image);
    }

    private int parsePort(String portStr) {
        try {
            if (portStr.contains("/")) {
                return Integer.parseInt(portStr.split("/")[0]);
            }
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return 80;
        }
    }
}

/**
 * Manager for AgentRun sessions that handles creation, retrieval,
 * updating, and deletion of sessions.
 */
class AgentRunSessionManager {
    private static final Logger logger = Logger.getLogger(AgentRunSessionManager.class.getName());
    private final Map<String, Map<String, Object>> sessions;

    public AgentRunSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        logger.fine("AgentRunSessionManager initialized");
    }

    public void createSession(String sessionId, Map<String, Object> sessionData) {
        sessions.put(sessionId, sessionData);
        logger.info("Created AgentRun session: " + sessionId);
    }

    public Map<String, Object> getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void updateSession(String sessionId, Map<String, Object> updates) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session != null) {
            session.putAll(updates);
            logger.fine("Updated AgentRun session: " + sessionId);
        }
    }

    public void deleteSession(String sessionId) {
        Map<String, Object> removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("Deleted AgentRun session: " + sessionId);
        }
    }

    public List<String> listSessions() {
        return new ArrayList<>(sessions.keySet());
    }
}
