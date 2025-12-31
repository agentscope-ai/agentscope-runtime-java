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
package io.agentscope.runtime.sandbox.manager.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for making remote calls to SandboxManager server.
 * This client handles the communication with the remote SandboxManager service.
 */
public class RemoteHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(RemoteHttpClient.class);
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String bearerToken;
    private final ObjectMapper objectMapper;
    
    public RemoteHttpClient(String baseUrl, String bearerToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bearerToken = bearerToken;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Make an HTTP request to the remote server
     * 
     * @param method HTTP method (GET, POST, DELETE, etc.)
     * @param endpoint Endpoint path (e.g., "/create", "/release")
     * @param data Request data
     * @param successKey Key in response JSON to extract data from
     * @return Response data
     */
    public Object makeRequest(RequestMethod method, String endpoint, Map<String, Object> data, String successKey) {
        String url = baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        logger.info("Making {} request to: {}", method, url);
        
        try {
            HttpRequest request = buildHttpRequest(method, url, data);
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String body = httpResponse.body();

            if (statusCode < 200 || statusCode >= 300) {
                String errorMessage = extractErrorMessage(body, statusCode);
                logger.error("HTTP error: {} - {}", statusCode, errorMessage);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", errorMessage);
                if (successKey != null && !successKey.isEmpty()) {
                    return null;
                }
                return errorResponse;
            }

            if (body == null || body.isEmpty()) {
                logger.warn("Response body is empty");
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(body, Map.class);

            if (successKey != null && !successKey.isEmpty()) {
                Object result = responseBody.get(successKey);
                logger.info("Request successful, extracted data with key: {}", successKey);
                return result;
            }

            return responseBody;

        } catch (Exception e) {
            logger.error("Error making request: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error: " + e.getMessage());
            if (successKey != null && !successKey.isEmpty()) {
                return null;
            }
            return errorResponse;
        }
    }
    
    /**
     * Build HttpRequest with method, headers and body
     */
    private HttpRequest buildHttpRequest(RequestMethod method, String url, Map<String, Object> data) throws Exception {
        HttpRequest.Builder builder;

        if (method == RequestMethod.GET || method == RequestMethod.HEAD) {
            String fullUrl = url + buildQueryString(data);
            builder = HttpRequest.newBuilder(URI.create(fullUrl));
            if (method == RequestMethod.HEAD) {
                builder = builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            } else {
                builder = builder.GET();
            }
        } else if (method == RequestMethod.DELETE) {
            builder = HttpRequest.newBuilder(URI.create(url)).DELETE();
        } else {
            String body = data == null ? "{}" : objectMapper.writeValueAsString(data);
            builder = HttpRequest.newBuilder(URI.create(url))
                .method(method.name(), HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        }

        if (bearerToken != null && !bearerToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        return builder.build();
    }
    
    /**
     * Extract error message from HTTP response body
     */
    private String extractErrorMessage(String responseBody, int statusCode) {
        try {
            if (responseBody != null && !responseBody.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
                if (errorMap.containsKey("detail")) {
                    return "Server Detail: " + errorMap.get("detail");
                } else if (errorMap.containsKey("error")) {
                    return "Server Error: " + errorMap.get("error");
                } else if (errorMap.containsKey("message")) {
                    return String.valueOf(errorMap.get("message"));
                }
                return responseBody;
            }
        } catch (Exception parseException) {
            logger.warn("Failed to parse error response: {}", parseException.getMessage());
        }
        return "HTTP " + statusCode + " Error";
    }

    private String buildQueryString(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        StringBuilder query = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                query.append("&");
            }
            first = false;
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String value = entry.getValue() == null ? "" : URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8);
            query.append(key).append("=").append(value);
        }
        return query.toString();
    }

    public String getBaseUrl() {
        return baseUrl;
    }
    
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isEmpty();
    }
}

