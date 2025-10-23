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
package io.agentscope.runtime.sandbox.manager.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Training Sandbox HTTP Client
 * Corresponds to Python's TrainingSandboxClient
 * Used for HTTP communication with training sandbox containers
 * 
 * <p>This client is specifically designed for training environments and provides
 * methods for creating instances, stepping through tasks, evaluating performance,
 * and managing training environment lifecycles.
 */
public class TrainingSandboxClient extends SandboxClient{
    private static final Logger logger = Logger.getLogger(TrainingSandboxClient.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final int timeout;
    
    /**
     * Constructor with ContainerModel
     * @param containerModel Container model with base URL information
     * @param timeout Timeout in seconds
     */
    public TrainingSandboxClient(ContainerModel containerModel, int timeout) {
        this(containerModel.getBaseUrl(), timeout);
    }
    
    /**
     * Constructor with base URL
     * @param baseUrl Base URL of the training sandbox service
     * @param timeout Timeout in seconds
     */
    public TrainingSandboxClient(String baseUrl, int timeout) {
        String[] urlSplit = baseUrl.split("/");
        StringBuilder baseUrlBuilder = new StringBuilder();
        for (int i = 0; i < urlSplit.length - 1; i++) {
            baseUrlBuilder.append(urlSplit[i]);
            if (i < urlSplit.length - 2) {
                baseUrlBuilder.append("/");
            }
        }
        this.baseUrl = baseUrlBuilder.toString();
        this.timeout = timeout;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        waitUntilHealthy();
    }
    
    /**
     * Check if the training sandbox service is healthy
     * @return true if healthy, false otherwise
     */
    public boolean checkHealth() {
        try {
            String endpoint = baseUrl + "/healthz";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            System.out.println(response);
            System.out.println(endpoint);
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Wait until the service is healthy
     */
    public void waitUntilHealthy() {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (checkHealth()) {
                logger.info("Training sandbox service is healthy: " + baseUrl);
                return;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for sandbox health", e);
            }
        }
        
        throw new RuntimeException("Training sandbox service did not start within timeout: " + timeout + "s");
    }
    
    /**
     * Make a POST request to the training sandbox
     * @param endpoint API endpoint (e.g., "create", "step")
     * @param envType Environment type
     * @param taskId Task ID
     * @param instanceId Instance ID
     * @param messages Messages or actions
     * @param params Additional parameters
     * @return Response as Map
     */
    private Map<String, Object> makeRequest(
            String endpoint,
            String envType,
            String taskId,
            String instanceId,
            Map<String, Object> messages,
            Map<String, Object> params) {
        try {
            String url = baseUrl + "/" + endpoint;
            
            Map<String, Object> requestBody = new HashMap<>();
            if (envType != null) requestBody.put("env_type", envType);
            if (taskId != null) requestBody.put("task_id", taskId);
            if (instanceId != null) requestBody.put("instance_id", instanceId);
            requestBody.put("messages", messages != null ? messages : new HashMap<>());
            requestBody.put("params", params != null ? params : new HashMap<>());
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.warning("Request failed: HTTP " + response.statusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "HTTP " + response.statusCode() + ": " + response.body());
                return error;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return result;
        } catch (Exception e) {
            logger.severe("Error making request to " + endpoint + ": " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }
    
    /**
     * Get task IDs for a specific environment
     * @param envType Environment type
     * @param split Data split (e.g., "train", "test")
     * @param params Additional parameters
     * @return Task IDs data
     */
    public Object getTaskIds(String envType, String split, Map<String, Object> params) {
        Map<String, Object> splitParams = params != null ? new HashMap<>(params) : new HashMap<>();
        splitParams.put("split", split);
        
        Map<String, Object> response = makeRequest(
                "get_env_profile",
                envType,
                null,
                null,
                null,
                splitParams
        );
        return response.get("data");
    }
    
    /**
     * Get environment profile
     * @param envType Environment type
     * @param split Data split
     * @param params Additional parameters
     * @return Environment profile data
     */
    public String getEnvProfile(String envType, String split, Map<String, Object> params) {
        Map<String, Object> splitParams = params != null ? new HashMap<>(params) : new HashMap<>();
        splitParams.put("split", split);
        
        Map<String, Object> response = makeRequest(
                "get_env_profile",
                envType,
                null,
                null,
                null,
                splitParams
        );
        try {
            return objectMapper.writeValueAsString(response.get("data"));
        } catch (Exception e) {
            logger.warning("Failed to serialize env profile data: " + e.getMessage());
            return response.get("data").toString();
        }
    }
    
    /**
     * Get tools information for an instance
     * @param instanceId Instance ID
     * @param messages Messages
     * @param params Parameters
     * @return Tools information
     */
    public String getToolsInfo(String instanceId, Map<String, Object> messages, Map<String, Object> params) {
        Map<String, Object> response = makeRequest(
                "get_info",
                null,
                null,
                instanceId,
                messages,
                params
        );
        try {
            return objectMapper.writeValueAsString(response.get("data"));
        } catch (Exception e) {
            logger.warning("Failed to serialize tools info data: " + e.getMessage());
            return response.get("data").toString();
        }
    }
    
    /**
     * Create a training instance
     * @param envType Environment type
     * @param taskId Task ID
     * @param instanceId Instance ID (optional)
     * @param params Additional parameters
     * @return Created instance information
     */
    public String createInstance(String envType, String taskId, String instanceId, Map<String, Object> params) {
        Map<String, Object> response = makeRequest(
                "create",
                envType,
                taskId,
                instanceId,
                null,
                params
        );
        try {
            return objectMapper.writeValueAsString(response.get("data"));
        } catch (Exception e) {
            logger.warning("Failed to serialize create instance data: " + e.getMessage());
            return response.get("data").toString();
        }
    }
    
    /**
     * Execute a step in the training environment
     * @param instanceId Instance ID
     * @param action Action to perform
     * @param params Additional parameters
     * @return Step result
     */
    public String step(String instanceId, Map<String, Object> action, Map<String, Object> params) {
        Map<String, Object> response = makeRequest(
                "step",
                null,
                null,
                instanceId,
                action,
                params
        );
        try {
            return objectMapper.writeValueAsString(response.get("data"));
        } catch (Exception e) {
            logger.warning("Failed to serialize step data: " + e.getMessage());
            return response.get("data").toString();
        }
    }
    
    /**
     * Evaluate instance performance
     * @param instanceId Instance ID
     * @param messages Evaluation messages
     * @param params Additional parameters
     * @return Evaluation results
     */
    public String evaluate(String instanceId, Map<String, Object> messages, Map<String, Object> params) {
        Map<String, Object> response = makeRequest(
                "evaluate",
                null,
                null,
                instanceId,
                messages,
                params
        );
        try {
            return objectMapper.writeValueAsString(response.get("data"));
        } catch (Exception e) {
            logger.warning("Failed to serialize evaluate data: " + e.getMessage());
            return response.get("data").toString();
        }
    }
    
    /**
     * Release a training instance
     * @param instanceId Instance ID
     * @return true if released successfully
     */
    public String releaseInstance(String instanceId) {
        Map<String, Object> response = makeRequest(
                "release",
                null,
                null,
                instanceId,
                null,
                null
        );
        Object success = response.get("success");
        return success != null && (boolean) success ? "success" : "failure";
    }
    
    /**
     * List available tools (for compatibility with SandboxHttpClient interface)
     * @param instanceId Instance ID
     * @param messages Messages
     * @param params Parameters
     * @return Tools information
     */
    public String listTools(String instanceId, Map<String, Object> messages, Map<String, Object> params) {
        if (instanceId != null && !instanceId.isEmpty()) {
            return getToolsInfo(instanceId, messages, params);
        }
        return null;
    }
    
    /**
     * Call a tool by name
     * @param name Tool name
     * @param arguments Arguments
     * @return Tool result
     */
    public String callTool(String name, Map<String, Object> arguments) {
        if (arguments == null) {
            return null;
        }
        
        switch (name) {
            case "create_instance":
                return createInstance(
                        (String) arguments.get("env_type"),
                        (String) arguments.get("task_id"),
                        (String) arguments.get("instance_id"),
                        getMapOrEmpty(arguments, "params")
                );
            case "release_instance":
                return releaseInstance((String) arguments.get("instance_id"));
            case "evaluate":
                return evaluate(
                        (String) arguments.get("instance_id"),
                        getMapOrEmpty(arguments, "messages"),
                        getMapOrEmpty(arguments, "params")
                );
            case "step":
                return step(
                        (String) arguments.get("instance_id"),
                        getMapOrEmpty(arguments, "action"),
                        getMapOrEmpty(arguments, "params")
                );
            case "get_task_ids":
            case "get_env_profile":
                return getEnvProfile(
                        (String) arguments.get("env_type"),
                        (String) arguments.getOrDefault("split", "train"),
                        getMapOrEmpty(arguments, "params")
                );
            default:
                logger.warning("Unknown tool name: " + name);
                return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapOrEmpty(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    public Map<String, Object> listTools(String toolType){
        // Todo: to be implemented
        return null;
    }
    
    @Override
    public void close() throws IOException {
        // HttpClient doesn't need explicit closing
    }
}

