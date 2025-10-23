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
 * Sandbox HTTP Client
 * Corresponds to Python's SandboxHttpClient
 * Used for HTTP communication with sandbox containers and calling tools within the sandbox
 */
public class SandboxHttpClient extends SandboxClient{
    private static final Logger logger = Logger.getLogger(SandboxHttpClient.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String sessionId;
    private final String baseUrl;
    private final String secret;
    private final HttpClient httpClient;
    private final int timeout;
    
    public SandboxHttpClient(ContainerModel containerModel, int timeout) {
        this.sessionId = containerModel.getSessionId();
        this.baseUrl = containerModel.getBaseUrl();
        this.secret = containerModel.getAuthToken();
        this.timeout = timeout;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        waitUntilHealthy();
    }
    
    public boolean checkHealth() {
        try {
            String endpoint = baseUrl + "/healthz";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + secret)
                    .header("x-agentrun-session-id", "s" + sessionId)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void waitUntilHealthy() {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (checkHealth()) {
                logger.info("Sandbox service is healthy: " + baseUrl);
                return;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for sandbox health", e);
            }
        }
        
        throw new RuntimeException("Sandbox service did not start within timeout: " + timeout + "s");
    }

    public Map<String, Object> listTools(String toolType) {
        try {
            String endpoint = baseUrl + "/mcp/list_tools";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + secret)
                    .header("Content-Type", "application/json")
                    .header("x-agentrun-session-id", "s" + sessionId)
                    .timeout(Duration.ofSeconds(timeout))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warning("Failed to list tools: HTTP " + response.statusCode());
                return new HashMap<>();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> tools = objectMapper.readValue(response.body(), Map.class);

            Map<String, Object> genericTools = getGenericToolsSchema();
            tools.put("generic", genericTools);

            if (toolType != null && !toolType.isEmpty()) {
                Map<String, Object> filtered = new HashMap<>();
                filtered.put(toolType, tools.getOrDefault(toolType, new HashMap<>()));
                return filtered;
            }

            return tools;
        } catch (Exception e) {
            logger.severe("Error listing tools: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    public String callTool(String toolName, Map<String, Object> arguments) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        
        try {
            if (isGenericTool(toolName)) {
                return callGenericTool(toolName, arguments);
            }
            
            String endpoint = baseUrl + "/mcp/call_tool";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("tool_name", toolName);
            requestBody.put("arguments", arguments);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + secret)
                    .header("Content-Type", "application/json")
                    .header("x-agentrun-session-id", "s" + sessionId)
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return createErrorResponse("HTTP " + response.statusCode() + ": " + response.body());
            }
            
            return response.body();
        } catch (Exception e) {
            logger.severe("Error calling tool " + toolName + ": " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Error calling tool: " + e.getMessage());
        }
    }
    
    private String callGenericTool(String toolName, Map<String, Object> arguments) {
        try {
            String endpoint = baseUrl + "/tools/" + toolName;
            
            String jsonBody = objectMapper.writeValueAsString(arguments);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + secret)
                    .header("Content-Type", "application/json")
                    .header("x-agentrun-session-id", "s" + sessionId)
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return createErrorResponse("HTTP " + response.statusCode() + ": " + response.body());
            }
            
            return response.body();
        } catch (Exception e) {
            logger.severe("Error calling generic tool " + toolName + ": " + e.getMessage());
            return createErrorResponse("Error calling generic tool: " + e.getMessage());
        }
    }
    
    private boolean isGenericTool(String toolName) {
        return "run_ipython_cell".equals(toolName) || "run_shell_command".equals(toolName);
    }
    
    private String createErrorResponse(String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("isError", true);
            Map<String, String> content = new HashMap<>();
            content.put("type", "text");
            content.put("text", errorMessage);
            error.put("content", new Map[]{content});
            return objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"" + errorMessage + "\"}]}";
        }
    }
    
    private Map<String, Object> getGenericToolsSchema() {
        Map<String, Object> tools = new HashMap<>();
        
        // run_ipython_cell
        Map<String, Object> ipythonTool = new HashMap<>();
        ipythonTool.put("name", "run_ipython_cell");
        Map<String, Object> ipythonSchema = new HashMap<>();
        ipythonSchema.put("type", "function");
        Map<String, Object> ipythonFunction = new HashMap<>();
        ipythonFunction.put("name", "run_ipython_cell");
        ipythonFunction.put("description", "Run an IPython cell.");
        Map<String, Object> ipythonParams = new HashMap<>();
        ipythonParams.put("type", "object");
        Map<String, Object> ipythonProps = new HashMap<>();
        Map<String, Object> codeProp = new HashMap<>();
        codeProp.put("type", "string");
        codeProp.put("description", "IPython code to execute");
        ipythonProps.put("code", codeProp);
        ipythonParams.put("properties", ipythonProps);
        ipythonParams.put("required", new String[]{"code"});
        ipythonFunction.put("parameters", ipythonParams);
        ipythonSchema.put("function", ipythonFunction);
        ipythonTool.put("json_schema", ipythonSchema);
        tools.put("run_ipython_cell", ipythonTool);
        
        // run_shell_command
        Map<String, Object> shellTool = new HashMap<>();
        shellTool.put("name", "run_shell_command");
        Map<String, Object> shellSchema = new HashMap<>();
        shellSchema.put("type", "function");
        Map<String, Object> shellFunction = new HashMap<>();
        shellFunction.put("name", "run_shell_command");
        shellFunction.put("description", "Run a shell command.");
        Map<String, Object> shellParams = new HashMap<>();
        shellParams.put("type", "object");
        Map<String, Object> shellProps = new HashMap<>();
        Map<String, Object> commandProp = new HashMap<>();
        commandProp.put("type", "string");
        commandProp.put("description", "Shell command to execute");
        shellProps.put("command", commandProp);
        shellParams.put("properties", shellProps);
        shellParams.put("required", new String[]{"command"});
        shellFunction.put("parameters", shellParams);
        shellSchema.put("function", shellFunction);
        shellTool.put("json_schema", shellSchema);
        tools.put("run_shell_command", shellTool);
        
        return tools;
    }
    
    @Override
    public void close() throws IOException {
        // HttpClient doesn't need explicit closing
    }
}

