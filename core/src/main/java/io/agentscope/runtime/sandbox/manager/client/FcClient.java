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

import com.aliyun.fc20230330.Client;
import com.aliyun.fc20230330.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import io.agentscope.runtime.sandbox.manager.client.config.FcClientConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FcClient extends BaseClient {
    private static final Logger logger = LoggerFactory.getLogger(FcClient.class);
    private static final String HTTPS_PROTOCOL = "https";
    private static final int HTTPS_PORT = 443;

    private Client fcClient;
    private FcClientConfig fcClientConfig;
    private FCSessionManager fcSessionManager;
    private String functionPrefix;
    private boolean connected = false;
    private final SecureRandom random = new SecureRandom();

    public FcClient(FcClientConfig fcClientConfig) throws Exception {
        this.fcClientConfig = fcClientConfig;
        this.fcSessionManager = new FCSessionManager();
        this.functionPrefix = fcClientConfig.getFcPrefix() != null ? fcClientConfig.getFcPrefix() : "agentscope-sandbox";
        this.fcClient = createFcClient();

        logger.info("FunctionComputeClient initialized successfully with config: {}", fcClientConfig);

        testConnection();
    }

    private Client createFcClient() throws Exception {
        Config fcConfig = new Config();
        fcConfig.setAccessKeyId(fcClientConfig.getFcAccessKeyId());
        fcConfig.setAccessKeySecret(fcClientConfig.getFcAccessKeySecret());
        String endpoint = String.format("%s.%s.fc.aliyuncs.com",
                fcClientConfig.getFcAccountId(),
                fcClientConfig.getFcRegionId());
        fcConfig.setEndpoint(endpoint);
        return new Client(fcConfig);
    }

    private void testConnection() {
        try {
            ListFunctionsRequest listRequest = new ListFunctionsRequest();
            listRequest.setLimit(1);

            ListFunctionsResponse response = fcClient.listFunctions(listRequest);

            if (response != null && response.getBody() != null && response.getBody().getFunctions() != null) {
                int funcCount = response.getBody().getFunctions().size();
                logger.info("FunctionComputeClient FC connection test successful: {} functions", funcCount);
                this.connected = true;
            }
        } catch (Exception e) {
            logger.warn("FunctionComputeClient FC connection test failed: {}", e.getMessage());
            logger.warn("FunctionComputeClient This may not affect normal usage. If there are permission issues, please check AccessKey permission configuration.");
        }
    }

    @Override
    public boolean connect() {
        testConnection();
        return connected;
    }

    @Override
    public boolean isConnected() {
        return connected && fcClient != null;
    }

    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName,
                                                 List<String> ports,
                                                 List<VolumeBinding> volumeBindings,
                                                 Map<String, String> environment,
                                                 Map<String, Object> runtimeConfig) {
        int port = 80;
        if (ports != null && !ports.isEmpty()) {
            String firstPort = ports.get(0);
            if ("80/tcp".equals(firstPort)) {
                port = 80;
            } else {
                // Extract port number from format like "8080/tcp" or just "8080"
                String portStr = firstPort.split("/")[0];
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    port = 80;
                }
            }
        }

        String fcImage = replaceFcImages(imageName);
        try {
            // 1. Generate session ID and function name
            String sessionId = containerName != null ? containerName : generateSessionId();
            String functionName = functionPrefix + "-" + sessionId;

            // 2. Build custom container configuration
            CustomContainerConfig customContainerConfig = new CustomContainerConfig();
            customContainerConfig.setImage(fcImage);
            customContainerConfig.setPort(port);
            customContainerConfig.setAccelerationType("Default");

            // Build health check configuration
            String healthCheckUrl = "/";
            CustomHealthCheckConfig healthCheckConfig = new CustomHealthCheckConfig();
            healthCheckConfig.setFailureThreshold(60);
            healthCheckConfig.setHttpGetUrl(healthCheckUrl);
            healthCheckConfig.setInitialDelaySeconds(2);
            healthCheckConfig.setPeriodSeconds(1);
            healthCheckConfig.setSuccessThreshold(1);
            healthCheckConfig.setTimeoutSeconds(1);
            customContainerConfig.setHealthCheckConfig(healthCheckConfig);

            logger.info("FunctionComputeClient building custom health check configuration: {}", healthCheckConfig);

            // 3. Build function creation parameters
            CreateFunctionInput createFunctionInput = new CreateFunctionInput();
            createFunctionInput.setFunctionName(functionName);
            createFunctionInput.setRuntime("custom-container");
            createFunctionInput.setCustomContainerConfig(customContainerConfig);
            createFunctionInput.setDescription("AgentScope Runtime Sandbox Function - " + sessionId);
            createFunctionInput.setTimeout(300);
            createFunctionInput.setMemorySize(fcClientConfig.getFcMemory());
            createFunctionInput.setDiskSize(512);
            createFunctionInput.setCpu(fcClientConfig.getFcCpu());
            createFunctionInput.setInstanceConcurrency(200);
            createFunctionInput.setInternetAccess(true);
            createFunctionInput.setEnvironmentVariables(environment != null ? environment : new HashMap<>());
            createFunctionInput.setSessionAffinity("HEADER_FIELD");
            createFunctionInput.setInstanceIsolationMode("SESSION_EXCLUSIVE");
            createFunctionInput.setSessionAffinityConfig("{\"affinityHeaderFieldName\":\"x-agentscope-runtime-session-id\",\"sessionTTLInSeconds\":21600,\"sessionConcurrencyPerInstance\":1,\"sessionIdleTimeoutInSeconds\":3600}");

            // 4. If log configuration exists
            if (fcClientConfig.getFcLogStore() != null && fcClientConfig.getFcLogProject() != null) {
                LogConfig logConfig = new LogConfig();
                logConfig.setLogstore(fcClientConfig.getFcLogStore());
                logConfig.setProject(fcClientConfig.getFcLogProject());
                logConfig.setEnableRequestMetrics(true);
                logConfig.setEnableInstanceMetrics(true);
                logConfig.setLogBeginRule("DefaultRegex");
                createFunctionInput.setLogConfig(logConfig);
                logger.info("Configuring log service: {}/{}", fcClientConfig.getFcLogProject(), fcClientConfig.getFcLogStore());
            }

            // 5. If VPC configuration exists
            if (fcClientConfig.getFcVpcId() != null && fcClientConfig.getFcVswitchIds() != null && fcClientConfig.getFcSecurityGroupId() != null) {
                VPCConfig vpcConfig = new VPCConfig();
                vpcConfig.setVpcId(fcClientConfig.getFcVpcId());
                vpcConfig.setVSwitchIds(fcClientConfig.getFcVswitchIds());
                vpcConfig.setSecurityGroupId(fcClientConfig.getFcSecurityGroupId());
                createFunctionInput.setVpcConfig(vpcConfig);
                logger.info("Configuring VPC network: {}", fcClientConfig.getFcVpcId());
            }

            // 6. Create function
            CreateFunctionRequest createFunctionRequest = new CreateFunctionRequest();
            createFunctionRequest.setBody(createFunctionInput);

            RuntimeOptions runtimeOptions = new RuntimeOptions();
            Map<String, String> headers = new HashMap<>();

            CreateFunctionResponse response = fcClient.createFunctionWithOptions(createFunctionRequest, headers, runtimeOptions);

            logger.info("FunctionComputeClient function created successfully!");
            logger.info("FunctionComputeClient function name: {}", response.getBody().getFunctionName());
            logger.info("FunctionComputeClient runtime: {}", response.getBody().getRuntime());
            logger.info("FunctionComputeClient create time: {}", response.getBody().getCreatedTime());

            // 7. Create HTTP trigger
            Map<String, String> triggerInfo = createHttpTrigger(functionName, sessionId);
            String triggerName = triggerInfo.get("trigger_name");
            String endpointInternetUrl = triggerInfo.get("url_internet");
            String endpointIntranetUrl = triggerInfo.get("url_intranet");

            // 8. Poll for function ready status
            while (!checkFunctionReady(functionName)) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for function to be ready", e);
                }
                logger.info("Check function deployment status, function name: {}", functionName);
            }

            // 9. Create session data
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("session_id", sessionId);
            sessionData.put("function_name", functionName);
            sessionData.put("trigger_name", triggerName);
            sessionData.put("trigger_id", triggerInfo.get("trigger_id"));
            sessionData.put("created_time", System.currentTimeMillis() / 1000.0);
            sessionData.put("status", "running");
            sessionData.put("image", fcImage);
            sessionData.put("ports", ports);
            sessionData.put("environment", environment);
            if (environment != null && environment.containsKey("SECRET_TOKEN")) {
                sessionData.put("runtime_token", environment.get("SECRET_TOKEN"));
            }
            sessionData.put("url_internet", endpointInternetUrl);
            sessionData.put("url_intranet", endpointIntranetUrl);

            // 10. Register session
            fcSessionManager.createSession(sessionId, sessionData);

            logger.info("FunctionComputeClient FC function {} created and registered session successfully", sessionId);

            // 11. Parse URL
            URI parsedUrl;
            try {
                parsedUrl = new URI(endpointInternetUrl);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid URL format: " + endpointInternetUrl, e);
            }

            String endpointPublicUrlDomain = parsedUrl.getHost();
            String endpointPublicUrlPath = parsedUrl.getPath();

            // FC should adapt for ip and port format
            List<String> resultPorts = new ArrayList<>();
            resultPorts.add(HTTPS_PORT + endpointPublicUrlPath);

            return new ContainerCreateResult(sessionId, resultPorts, endpointPublicUrlDomain, HTTPS_PROTOCOL);

        } catch (Exception e) {
            logger.error("Create FC function failed: {}", e.getMessage());
            // Provide more detailed error information
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("InvalidAccessKeyId")) {
                    logger.error("Authentication failed, please check if FC_ACCESS_KEY_ID is correct");
                } else if (errorMsg.contains("SignatureDoesNotMatch")) {
                    logger.error("Signature mismatch, please check if FC_ACCESS_KEY_SECRET is correct");
                } else if (errorMsg.contains("Forbidden")) {
                    logger.error("Insufficient permissions, please check if AccessKey has FC service permissions");
                }
            }
            throw new RuntimeException("FC function creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void startContainer(String containerId) {
        Map<String, Object> session = fcSessionManager.getSession(containerId);
        if (session == null) {
            logger.warn("FunctionComputeClient session record not found: {}", containerId);
            return;
        }

        int timeoutSeconds = 300;
        int interval = 2;
        int maxRetries = timeoutSeconds / interval;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for FC function to start");
                return;
            }
            String functionStatus = getContainerStatus(containerId);

            if ("running".equals(functionStatus)) {
                break;
            }

            logger.info("FunctionComputeClient waiting for FC function to be ready... ({}/{}) | Current status: {}, session: {}",
                    i + 1, maxRetries, functionStatus, containerId);
        }

        String finalStatus = getContainerStatus(containerId);
        if (!"running".equals(finalStatus)) {
            logger.warn("FunctionComputeClient start timeout: Waiting for FC function to enter running state exceeded {} seconds, final status: {}, session: {}",
                    timeoutSeconds, finalStatus, containerId);
            return;
        }

        // Update session status to running
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "running");
        fcSessionManager.updateSession(containerId, updates);
        logger.info("FunctionComputeClient FC function started: {}", containerId);
    }

    @Override
    public void stopContainer(String containerId) {
        Map<String, Object> session = fcSessionManager.getSession(containerId);
        if (session == null) {
            logger.warn("FunctionComputeClient session record not found: {}", containerId);
            return;
        }

        try {
            // FC functions cannot be directly stopped, only update session status
            // TODO Need to call function disable interface
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "stopped");
            fcSessionManager.updateSession(containerId, updates);
            logger.info("FunctionComputeClient FC function status set to stopped: {}", containerId);
        } catch (Exception e) {
            logger.error("FunctionComputeClient stop FC function failed {}: {}", containerId, e.getMessage());
        }
    }

    @Override
    public void removeContainer(String containerId) {
        removeContainer(containerId, false);
    }

    public void removeContainer(String containerId, boolean force) {
        Map<String, Object> session = fcSessionManager.getSession(containerId);
        if (session == null) {
            logger.warn("FunctionComputeClient session record not found, skipping deletion: {}", containerId);
            return;
        }

        String functionName = (String) session.get("function_name");
        String triggerName = (String) session.get("trigger_name");

        try {
            logger.info("FunctionComputeClient starting to delete FC function: {}", functionName);

            // 1. Delete trigger first (if exists)
            if (triggerName != null) {
                try {
                    fcClient.deleteTriggerWithOptions(functionName, triggerName, new HashMap<>(), new RuntimeOptions());
                    logger.info("FunctionComputeClient trigger deleted: {}", triggerName);
                } catch (Exception triggerError) {
                    logger.warn("FunctionComputeClient delete trigger failed (continuing to delete function): {}", triggerError.getMessage());
                }
            }

            // 2. Delete function
            fcClient.deleteFunctionWithOptions(functionName, new HashMap<>(), new RuntimeOptions());

            // 3. Delete session record
            fcSessionManager.deleteSession(containerId);

            logger.info("FunctionComputeClient FC function deleted successfully: {}", functionName);
        } catch (Exception e) {
            logger.error("FunctionComputeClient delete FC function failed: {}", e.getMessage());
            if (force) {
                // Force cleanup - delete session record even if API call fails
                fcSessionManager.deleteSession(containerId);
                logger.warn("FunctionComputeClient force delete session record: {}", containerId);
            } else {
                throw new RuntimeException("FunctionComputeClient FC function removal failed: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String getContainerStatus(String containerId) {
        Map<String, Object> session = fcSessionManager.getSession(containerId);
        if (session == null) {
            logger.warn("FunctionComputeClient session record not found: {}", containerId);
            return "not_found";
        }

        String functionName = (String) session.get("function_name");
        String status = getFunctionStatus(functionName);
        if ("unknown".equals(status)) {
            return (String) session.getOrDefault("status", "unknown");
        }
        return status;
    }

    @Override
    public boolean imageExists(String imageName) {
        // FC doesn't need to check image existence locally
        return true;
    }

    @Override
    public boolean inspectContainer(String containerIdOrName) {
        Map<String, Object> session = fcSessionManager.getSession(containerIdOrName);
        if (session == null) {
            logger.warn("FunctionComputeClient session record not found: {}", containerIdOrName);
            return false;
        }

        String functionName = (String) session.get("function_name");
        try {
            GetFunctionRequest functionQueryRequest = new GetFunctionRequest();
            functionQueryRequest.setQualifier("LATEST");

            GetFunctionResponse response = fcClient.getFunction(functionName, functionQueryRequest);
            logger.info("FunctionComputeClient function inspect: {}", response);

            return response != null && response.getBody() != null;
        } catch (Exception e) {
            logger.error("FunctionComputeClient get FC function information failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean pullImage(String imageName) {
        return true;
    }

    private boolean checkFunctionReady(String functionName) {
        try {
            String status = getFunctionStatus(functionName);
            return "running".equals(status);
        } catch (Exception e) {
            logger.error("Error checking function status: {}", e.getMessage());
            return false;
        }
    }

    private String getFunctionStatus(String functionName) {
        try {
            GetFunctionRequest functionQueryRequest = new GetFunctionRequest();
            functionQueryRequest.setQualifier("LATEST");
            GetFunctionResponse response = fcClient.getFunction(functionName, functionQueryRequest);

            // Return function status (Active, Inactive, Pending)
            if (response != null && response.getBody() != null && response.getBody().getState() != null) {
                String state = response.getBody().getState().toLowerCase();
                if ("active".equals(state)) {
                    return "running";
                } else if ("inactive".equals(state)) {
                    return "exited";
                } else if ("pending".equals(state)) {
                    return "creating";
                } else {
                    return state;
                }
            } else {
                return "unknown";
            }
        } catch (Exception e) {
            logger.error("FunctionComputeClient get FC function status failed: {}", e.getMessage());
            // If API call fails, return status from session
            return "unknown";
        }
    }

    private String generateSessionId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    private Map<String, String> createHttpTrigger(String functionName, String sessionId) {
        String triggerName = "sandbox-http-trigger-" + sessionId;

        try {
            logger.info("FunctionComputeClient creating HTTP trigger: {}", triggerName);

            // Build trigger configuration as JSON string
            // Format: {"authType":"anonymous","methods":["GET","POST","PUT","DELETE","HEAD","OPTIONS"]}
            String triggerConfigJson = "{\"authType\":\"anonymous\",\"methods\":[\"GET\",\"POST\",\"PUT\",\"DELETE\",\"HEAD\",\"OPTIONS\"]}";

            // Create trigger input
            CreateTriggerInput triggerInput = new CreateTriggerInput();
            triggerInput.setTriggerName(triggerName);
            triggerInput.setTriggerType("http");
            triggerInput.setTriggerConfig(triggerConfigJson);
            triggerInput.setDescription("HTTP trigger for sandbox session " + sessionId);

            // Create trigger request
            CreateTriggerRequest createTriggerRequest = new CreateTriggerRequest();
            createTriggerRequest.setBody(triggerInput);

            // Call API to create trigger
            RuntimeOptions runtime = new RuntimeOptions();
            CreateTriggerResponse response = fcClient.createTriggerWithOptions(functionName, createTriggerRequest, new HashMap<>(), runtime);

            logger.info("FunctionComputeClient HTTP trigger created successfully: {}", triggerName);
            logger.info("FunctionComputeClient HTTP trigger response: {}", response);

            // Extract trigger information from response
            Map<String, String> triggerInfo = new HashMap<>();
            triggerInfo.put("trigger_name", triggerName);
            triggerInfo.put("url_internet", null);
            triggerInfo.put("url_intranet", null);
            triggerInfo.put("trigger_id", null);
            triggerInfo.put("qualifier", "LATEST");
            triggerInfo.put("last_modified_time", null);
            triggerInfo.put("created_time", null);
            triggerInfo.put("status", null);

            if (response != null && response.getBody() != null) {
                Trigger body = response.getBody();
                HTTPTrigger httpTriggerObj = body.getHttpTrigger();
                if (httpTriggerObj != null) {
                    String urlInternet = httpTriggerObj.getUrlInternet();
                    String urlIntranet = httpTriggerObj.getUrlIntranet();
                    triggerInfo.put("url_internet", urlInternet);
                    triggerInfo.put("url_intranet", urlIntranet);
                }

                String triggerId = body.getTriggerId();
                triggerInfo.put("trigger_id", triggerId);

                String lastModifiedTime = body.getLastModifiedTime();
                triggerInfo.put("last_modified_time", lastModifiedTime);

                String createdTime = body.getCreatedTime();
                triggerInfo.put("created_time", createdTime);

                String status = body.getStatus();
                triggerInfo.put("status", status);

                String qualifier = body.getQualifier();
                triggerInfo.put("qualifier", qualifier);
            }

            logger.info("FunctionComputeClient trigger URL information:");
            logger.info("FunctionComputeClient   - Internet URL: {}", triggerInfo.get("url_internet"));
            logger.info("FunctionComputeClient   - Intranet URL: {}", triggerInfo.get("url_intranet"));
            logger.info("FunctionComputeClient   - Trigger ID: {}", triggerInfo.get("trigger_id"));

            return triggerInfo;
        } catch (Exception e) {
            logger.error("FunctionComputeClient create HTTP trigger failed: {}", e.getMessage());
            // Even if creation fails, return basic information for subsequent cleanup
            Map<String, String> triggerInfo = new HashMap<>();
            triggerInfo.put("trigger_name", triggerName);
            triggerInfo.put("url_internet", null);
            triggerInfo.put("url_intranet", null);
            triggerInfo.put("qualifier", "LATEST");
            triggerInfo.put("latest_modified_time", null);
            triggerInfo.put("created_time", null);
            triggerInfo.put("status", null);
            return triggerInfo;
        }
    }

    private String replaceFcImages(String image) {
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
            imageName = image.split(":")[0];
        } else {
            imageName = image;
        }

        return replacementMap.getOrDefault(imageName.trim(), image);
    }

}

class FCSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(FCSessionManager.class);
    private final Map<String, Map<String, Object>> sessionMap = new ConcurrentHashMap<>();

    public void createSession(String sessionId, Map<String, Object> sessionData) {
        this.sessionMap.put(sessionId, sessionData);
        logger.info("Created FC session: {}", sessionId);
    }

    public Map<String, Object> getSession(String sessionId) {
        return this.sessionMap.get(sessionId);
    }

    public void updateSession(String sessionId, Map<String, Object> updates) {
        Map<String, Object> session = this.sessionMap.get(sessionId);
        if (session != null) {
            session.putAll(updates);
            logger.info("Updated FC session: {}", sessionId);
        }
    }

    public void deleteSession(String sessionId) {
        this.sessionMap.remove(sessionId);
        logger.info("Deleted FC session: {}", sessionId);
    }

    public List<String> listSessions() {
        return new ArrayList<>(this.sessionMap.keySet());
    }
}