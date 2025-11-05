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
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * HTTP client for making remote calls to SandboxManager server.
 * This client handles the communication with the remote SandboxManager service.
 */
public class RemoteHttpClient {
    
    private static final Logger logger = Logger.getLogger(RemoteHttpClient.class.getName());
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String bearerToken;
    private final ObjectMapper objectMapper;
    
    public RemoteHttpClient(String baseUrl, String bearerToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bearerToken = bearerToken;
        this.restTemplate = new RestTemplate();
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
        
        logger.info("Making " + method + " request to: " + url);
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null && !bearerToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + bearerToken);
        }
        
        try {
            HttpEntity<Map<String, Object>> requestEntity;
            ResponseEntity<Map<String, Object>> response;
            
            if (method == RequestMethod.GET) {
                requestEntity = new HttpEntity<>(headers);
                StringBuilder queryString = new StringBuilder();
                if (data != null && !data.isEmpty()) {
                    queryString.append("?");
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        if (queryString.length() > 1) {
                            queryString.append("&");
                        }
                        queryString.append(entry.getKey()).append("=").append(entry.getValue());
                    }
                }
                url += queryString.toString();
                org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = 
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
                response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, typeRef);
            } else {
                requestEntity = new HttpEntity<>(data, headers);
                HttpMethod httpMethod = HttpMethod.valueOf(method.name());
                org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = 
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
                response = restTemplate.exchange(url, httpMethod, requestEntity, typeRef);
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                logger.warning("Response body is null");
                return null;
            }
            
            if (successKey != null && !successKey.isEmpty()) {
                Object result = responseBody.get(successKey);
                logger.info("Request successful, extracted data with key: " + successKey);
                return result;
            }
            
            return responseBody;
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.severe("HTTP error: " + e.getStatusCode() + " - " + e.getMessage());
            
            String errorMessage = extractErrorMessage(e);
            logger.severe("Error details: " + errorMessage);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            if (successKey != null && !successKey.isEmpty()) {
                return errorResponse.get(successKey);
            }
            return errorResponse;
            
        } catch (Exception e) {
            logger.severe("Error making request: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error: " + e.getMessage());
            if (successKey != null && !successKey.isEmpty()) {
                return errorResponse.get(successKey);
            }
            return errorResponse;
        }
    }
    
    /**
     * Extract error message from HTTP exception
     */
    private String extractErrorMessage(Exception e) {
        try {
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException httpException = (HttpClientErrorException) e;
                String responseBody = httpException.getResponseBodyAsString();
                
                if (responseBody != null && !responseBody.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
                    
                    if (errorMap.containsKey("detail")) {
                        return "Server Detail: " + errorMap.get("detail");
                    } else if (errorMap.containsKey("error")) {
                        return "Server Error: " + errorMap.get("error");
                    } else {
                        return "Server Response: " + responseBody;
                    }
                }
            } else if (e instanceof HttpServerErrorException) {
                HttpServerErrorException httpException = (HttpServerErrorException) e;
                return "Server Error " + httpException.getStatusCode() + ": " + httpException.getMessage();
            }
        } catch (Exception parseException) {
            logger.warning("Failed to parse error response: " + parseException.getMessage());
        }
        
        return e.getMessage();
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isEmpty();
    }
}

