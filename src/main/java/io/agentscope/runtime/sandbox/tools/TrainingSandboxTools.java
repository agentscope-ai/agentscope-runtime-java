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
package io.agentscope.runtime.sandbox.tools;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.sandbox.manager.model.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.manager.util.HttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Training Sandbox Tools for managing and executing training-related tasks.
 * This class provides methods to create, manage, and interact with training environment instances.
 */
public class TrainingSandboxTools extends BaseSandboxTools {
    private static final Logger logger = Logger.getLogger(TrainingSandboxTools.class.getName());

    public TrainingSandboxTools() {
        super(Runner.getSandboxManager(), new HttpClient());
    }

    public String call_mcp_tool(SandboxType sandboxType, String envType, String toolName, String taskID, String instanceID, Map<String, String> messages, Map<String, String> params, String userID, String sessionID) {
        try {
            ContainerModel sandbox = sandboxManager.getSandbox(sandboxType, userID, sessionID);

            if (!isSandboxRunning(sandboxType, userID, sessionID)) {
                logger.info("Sandbox is not running, starting...");
                sandboxManager.startSandbox(sandboxType, userID, sessionID);
            }

            String baseUrl = sandbox.getBaseUrl();

            String[] urlSplit = baseUrl.split("/");
            StringBuilder baseUrlBuilder = new StringBuilder();
            for (int i = 0; i < urlSplit.length - 1; i++) {
                baseUrlBuilder.append(urlSplit[i]);
                if (i < urlSplit.length - 2) {
                    baseUrlBuilder.append("/");
                }
            }
            baseUrl = baseUrlBuilder.toString();
            String authToken = sandbox.getAuthToken();
            String requestUrl = baseUrl + toolName;

            // Health check wait
            waitUntilHealthy(sandbox, baseUrl);

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + authToken);
            headers.put("Content-Type", "application/json");
            headers.put("Host", "localhost:" + sandbox.getPorts()[0]);

            Map<String, Object> body = new HashMap<>();
            body.put("env_type", envType);
            body.put("task_id", taskID);
            body.put("instance_id", instanceID);
            body.put("messages", messages == null ? new HashMap<>() : messages);
            body.put("params", params == null ? new HashMap<>() : params);
            return httpClient.postJson(requestUrl, headers, body);
        } catch (Exception e) {
            String errorMsg = "Call MCP Tool Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String getTaskIDs(SandboxType sandboxType, String envType, String split, Map<String, Object> params, String userID, String sessionID) {
        if (split == null) {
            split = "train";
        }
        return call_mcp_tool(sandboxType, envType, "/get_env_profile", null, null, null, Map.of("split", split), userID, sessionID);
    }

    public String getEnvProfiles(SandboxType sandboxType, String envType, String split, Map<String, Object> params, String userID, String sessionID) {
        if (split == null) {
            split = "train";
        }
        return call_mcp_tool(sandboxType, envType, "/get_env_profile", null, null, null, Map.of("split", split), userID, sessionID);
    }

    public String getToolsInfo(String instanceId, Map<String, String> messages, Map<String, String> params, String userID, String sessionID) {
        return call_mcp_tool(SandboxType.TRAINING, "default", "get_info", null, instanceId, messages, params, userID, sessionID);
    }

    public String createInstance(String envType, String taskID, String instanceID, Map<String, String> params, String userID, String sessionID) {
        return call_mcp_tool(SandboxType.TRAINING, envType, "/create", taskID, instanceID, null, params, userID, sessionID);
    }

    public String step(String instanceID, Map<String, String> action, Map<String, String> params, String userID, String sessionID) {
        return call_mcp_tool(SandboxType.TRAINING, "default", "/step", null, instanceID, action, params, userID, sessionID);
    }

    public String evaluate(String instanceId, Map<String, String> messages, Map<String, String> params, String userID, String sessionID) {
        return call_mcp_tool(SandboxType.TRAINING, "default", "/evaluate", null, instanceId, messages, params, userID, sessionID);
    }

    public String releaseInstance(String instanceId, String userID, String sessionID) {
        return call_mcp_tool(SandboxType.TRAINING, "default", "/release", null, instanceId, null, null, userID, sessionID);
    }

    public String addMcpServers() {
//        Todo: to be implemented
        return null;
    }

    public String listTools(Map<String, Object> args) {
        if (args.containsKey("instance_id")) {
            return getToolsInfo(
                    (String) args.get("instance_id"),
                    (Map<String, String>) args.get("messages"),
                    (Map<String, String>) args.get("params"),
                    (String) args.get("user_id"),
                    (String) args.get("session_id")
            );
        }
        return null;
    }
}
