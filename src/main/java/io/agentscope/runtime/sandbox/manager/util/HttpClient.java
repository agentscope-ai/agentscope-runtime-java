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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP client utility class for sending HTTP requests
 */
public class HttpClient {
    
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public HttpClient() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Send POST request
     *
     * @param url request URL
     * @param headers request headers
     * @param requestBody request body
     * @return response content
     * @throws IOException request exception
     */
    public String post(String url, Map<String, String> headers, String requestBody) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        
        // Set request headers
        if (headers != null) {
            headers.forEach(httpPost::setHeader);
        }
        
        // Set request body
        if (requestBody != null) {
            StringEntity entity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
        }
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP request failed, status code: " + statusCode + ", response: " + responseBody);
            }
            return responseBody;
        }
    }
    
    /**
     * Send JSON POST request
     *
     * @param url request URL
     * @param headers request headers
     * @param requestData request data object
     * @return response content
     * @throws IOException request exception
     */
    public String postJson(String url, Map<String, String> headers, Object requestData) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestData);
        return post(url, headers, jsonBody);
    }

    /**
     * Send GET request
     *
     * @param url request URL
     * @param headers request headers
     * @return response content
     * @throws IOException request exception
     */
    public String get(String url, Map<String, String> headers) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        headers = null;
        if (headers != null) {
            headers.forEach(httpGet::setHeader);
        }
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8) : "";
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP request failed, status code: " + statusCode + ", response: " + responseBody);
            }
            return responseBody;
        }
    }
    
    /**
     * Close HTTP client
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
