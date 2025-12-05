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
package io.agentscope.runtime.engine.services.memory.persistence.memory.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agentscope.runtime.engine.schemas.Content;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.schemas.MessageType;
import io.agentscope.runtime.engine.schemas.Role;
import io.agentscope.runtime.engine.schemas.TextContent;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

/**
 * Mem0-based implementation of memory service
 * Uses Mem0 HTTP API for persistent storage with search capabilities
 * To get the API key, please refer to <a href="https://docs.mem0.ai/platform/quickstart">Mem0 Documentation</a>
 */
public class Mem0MemoryService implements MemoryService {

    private static final Logger logger = Logger.getLogger(Mem0MemoryService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MEM0_API_BASE_URL = "https://api.mem0.ai";
    private static final String API_VERSION_V1 = "v1";
    private static final String API_VERSION_V2 = "v2";

    private final String apiKey;
    private final CloseableHttpClient httpClient;
    private final String orgId;
    private final String projectId;

    /**
     * Constructor with API key
     *
     * @param apiKey Mem0 API key
     */
    public Mem0MemoryService(String apiKey) {
        this(apiKey, null, null);
    }

    /**
     * Constructor with API key and organization/project IDs
     *
     * @param apiKey Mem0 API key
     * @param orgId Organization ID (optional)
     * @param projectId Project ID (optional)
     */
    public Mem0MemoryService(String apiKey, String orgId, String projectId) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("MEM0_API_KEY is required");
        }
        this.apiKey = apiKey;
        this.orgId = orgId;
        this.projectId = projectId;
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> logger.info("Mem0 memory service started"));
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
                logger.info("Mem0 memory service stopped");
            } catch (Exception e) {
                logger.severe("Failed to close HTTP client: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try to list memories to check connection
                String url = String.format("%s/%s/memories/", MEM0_API_BASE_URL, API_VERSION_V2);
                HttpPost request = new HttpPost(url);
                request.setHeader("Authorization", "Token " + apiKey);
                request.setHeader("Content-Type", "application/json");

                ObjectNode body = OBJECT_MAPPER.createObjectNode();
                body.putObject("filters");
                if (orgId != null) {
                    body.put("org_id", orgId);
                }
                if (projectId != null) {
                    body.put("project_id", projectId);
                }

                request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    return response.getCode() == 200;
                }
            } catch (Exception e) {
                logger.severe("Mem0 memory service health check failed: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            try {
                String url = String.format("%s/%s/memories/", MEM0_API_BASE_URL, API_VERSION_V1);
                HttpPost request = new HttpPost(url);
                request.setHeader("Authorization", "Token " + apiKey);
                request.setHeader("Content-Type", "application/json");

                ObjectNode body = OBJECT_MAPPER.createObjectNode();

                // Transform messages to Mem0 format
                ArrayNode messagesArray = OBJECT_MAPPER.createArrayNode();
                for (Message message : messages) {
                    ObjectNode msgObj = transformMessageToMem0Format(message);
                    if (msgObj != null) {
                        messagesArray.add(msgObj);
                    }
                }
                body.set("messages", messagesArray);

                body.put("user_id", userId);
                sessionId.ifPresent(s -> body.put("run_id", s));
                if (orgId != null) {
                    body.put("org_id", orgId);
                }
                if (projectId != null) {
                    body.put("project_id", projectId);
                }
                body.put("version", "v2");

                request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        logger.fine("Added " + messages.size() + " messages to Mem0 for user: " + userId);
                    } else {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        logger.warning("Failed to add memory: " + statusCode + " - " + responseBody);
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to add memory: " + e.getMessage());
                throw new RuntimeException("Failed to add memory", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages,
                                                         Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }

            try {
                Message lastMessage = messages.get(messages.size() - 1);
                String query = getQueryText(lastMessage);

                if (query == null || query.trim().isEmpty()) {
                    return Collections.emptyList();
                }

                String url = String.format("%s/%s/memories/search/", MEM0_API_BASE_URL, API_VERSION_V2);
                HttpPost request = new HttpPost(url);
                request.setHeader("Authorization", "Token " + apiKey);
                request.setHeader("Content-Type", "application/json");

                ObjectNode body = OBJECT_MAPPER.createObjectNode();
                body.put("query", query);

                // Build filters
                ObjectNode filtersNode = OBJECT_MAPPER.createObjectNode();
                ArrayNode andFilters = OBJECT_MAPPER.createArrayNode();

                ObjectNode userFilter = OBJECT_MAPPER.createObjectNode();
                userFilter.put("user_id", userId);
                andFilters.add(userFilter);

                if (filters.isPresent()) {
                    for (Map.Entry<String, Object> entry : filters.get().entrySet()) {
                        if (!entry.getKey().equals("top_k") && !entry.getKey().equals("rerank")) {
                            ObjectNode filterNode = OBJECT_MAPPER.createObjectNode();
                            filterNode.put(entry.getKey(), entry.getValue().toString());
                            andFilters.add(filterNode);
                        }
                    }
                }

                filtersNode.set("AND", andFilters);
                body.set("filters", filtersNode);

                // Set top_k
                int topK = 100;
                if (filters.isPresent() && filters.get().containsKey("top_k")) {
                    Object topKObj = filters.get().get("top_k");
                    if (topKObj instanceof Integer) {
                        topK = (Integer) topKObj;
                    }
                }
                body.put("top_k", topK);

                // Set rerank
                boolean rerank = true;
                if (filters.isPresent() && filters.get().containsKey("rerank")) {
                    Object rerankObj = filters.get().get("rerank");
                    if (rerankObj instanceof Boolean) {
                        rerank = (Boolean) rerankObj;
                    }
                }
                body.put("rerank", rerank);

                if (orgId != null) {
                    body.put("org_id", orgId);
                }
                if (projectId != null) {
                    body.put("project_id", projectId);
                }

                request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    if (response.getCode() == 200) {
                        return parseSearchResponse(responseBody);
                    } else {
                        logger.warning("Failed to search memory: " + response.getCode() + " - " + responseBody);
                        return Collections.emptyList();
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to search memory: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/%s/memories/", MEM0_API_BASE_URL, API_VERSION_V2);
                HttpPost request = new HttpPost(url);
                request.setHeader("Authorization", "Token " + apiKey);
                request.setHeader("Content-Type", "application/json");

                ObjectNode body = OBJECT_MAPPER.createObjectNode();

                // Build filters
                ObjectNode filtersNode = OBJECT_MAPPER.createObjectNode();
                ArrayNode andFilters = OBJECT_MAPPER.createArrayNode();

                ObjectNode userFilter = OBJECT_MAPPER.createObjectNode();
                userFilter.put("user_id", userId);
                andFilters.add(userFilter);

                filtersNode.set("AND", andFilters);
                body.set("filters", filtersNode);

                if (orgId != null) {
                    body.put("org_id", orgId);
                }
                if (projectId != null) {
                    body.put("project_id", projectId);
                }

                request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    if (response.getCode() == 200) {
                        List<Message> allMessages = parseListResponse(responseBody);

                        // Manual pagination
                        if (filters.isPresent()) {
                            Map<String, Object> filterMap = filters.get();
                            int pageNum = (Integer) filterMap.getOrDefault("page_num", 1);
                            int pageSize = (Integer) filterMap.getOrDefault("page_size", 10);

                            if (pageNum < 1 || pageSize < 1) {
                                throw new IllegalArgumentException("page_num and page_size must be greater than 0");
                            }

                            int startIndex = (pageNum - 1) * pageSize;
                            int endIndex = Math.min(startIndex + pageSize, allMessages.size());

                            if (startIndex >= allMessages.size()) {
                                return Collections.emptyList();
                            }

                            return allMessages.subList(startIndex, endIndex);
                        }

                        return allMessages;
                    } else {
                        logger.warning("Failed to list memory: " + response.getCode() + " - " + responseBody);
                        return Collections.emptyList();
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to list memory: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // First, get all memories for the user/session
                String listUrl = String.format("%s/%s/memories/", MEM0_API_BASE_URL, API_VERSION_V2);
                HttpPost listRequest = new HttpPost(listUrl);
                listRequest.setHeader("Authorization", "Token " + apiKey);
                listRequest.setHeader("Content-Type", "application/json");

                ObjectNode listBody = OBJECT_MAPPER.createObjectNode();
                ObjectNode filtersNode = OBJECT_MAPPER.createObjectNode();
                ArrayNode andFilters = OBJECT_MAPPER.createArrayNode();

                ObjectNode userFilter = OBJECT_MAPPER.createObjectNode();
                userFilter.put("user_id", userId);
                andFilters.add(userFilter);

                if (sessionId.isPresent()) {
                    ObjectNode sessionFilter = OBJECT_MAPPER.createObjectNode();
                    sessionFilter.put("run_id", sessionId.get());
                    andFilters.add(sessionFilter);
                }

                filtersNode.set("AND", andFilters);
                listBody.set("filters", filtersNode);

                if (orgId != null) {
                    listBody.put("org_id", orgId);
                }
                if (projectId != null) {
                    listBody.put("project_id", projectId);
                }

                listRequest.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(listBody), ContentType.APPLICATION_JSON));

                List<String> memoryIds = new ArrayList<>();
                try (CloseableHttpResponse listResponse = httpClient.execute(listRequest)) {
                    String responseBody = EntityUtils.toString(listResponse.getEntity());
                    if (listResponse.getCode() == 200) {
                        JsonNode memories = OBJECT_MAPPER.readTree(responseBody);
                        if (memories.isArray()) {
                            for (JsonNode memory : memories) {
                                if (memory.has("id")) {
                                    memoryIds.add(memory.get("id").asText());
                                }
                            }
                        }
                    }
                }

                // Delete each memory
                for (String memoryId : memoryIds) {
                    String deleteUrl = String.format("%s/%s/memories/%s/", MEM0_API_BASE_URL, API_VERSION_V1, memoryId);
                    HttpDelete deleteRequest = new HttpDelete(deleteUrl);
                    deleteRequest.setHeader("Authorization", "Token " + apiKey);

                    try (CloseableHttpResponse deleteResponse = httpClient.execute(deleteRequest)) {
                        if (deleteResponse.getCode() >= 200 && deleteResponse.getCode() < 300) {
                            logger.fine("Deleted memory: " + memoryId);
                        } else {
                            String responseBody = EntityUtils.toString(deleteResponse.getEntity());
                            logger.warning("Failed to delete memory " + memoryId + ": " + deleteResponse.getCode() + " - " + responseBody);
                        }
                    }
                }

                String sessionInfo = sessionId.map(s -> ", session: " + s).orElse("");
                logger.fine("Deleted " + memoryIds.size() + " memories for user: " + userId + sessionInfo);
            } catch (Exception e) {
                logger.severe("Failed to delete memory: " + e.getMessage());
                throw new RuntimeException("Failed to delete memory", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/%s/memories/", MEM0_API_BASE_URL, API_VERSION_V2);
                HttpPost request = new HttpPost(url);
                request.setHeader("Authorization", "Token " + apiKey);
                request.setHeader("Content-Type", "application/json");

                ObjectNode body = OBJECT_MAPPER.createObjectNode();
                body.putObject("filters");
                if (orgId != null) {
                    body.put("org_id", orgId);
                }
                if (projectId != null) {
                    body.put("project_id", projectId);
                }

                request.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    if (response.getCode() == 200) {
                        Set<String> userIds = new HashSet<>();
                        JsonNode memories = OBJECT_MAPPER.readTree(responseBody);
                        if (memories.isArray()) {
                            for (JsonNode memory : memories) {
                                if (memory.has("user_id")) {
                                    userIds.add(memory.get("user_id").asText());
                                }
                            }
                        }
                        return new ArrayList<>(userIds);
                    } else {
                        logger.warning("Failed to get all users: " + response.getCode() + " - " + responseBody);
                        return Collections.emptyList();
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to get all users: " + e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Transform Message to Mem0 format
     */
    private ObjectNode transformMessageToMem0Format(Message message) {
        if (message == null) {
            return null;
        }

        ObjectNode msgObj = OBJECT_MAPPER.createObjectNode();

        // Extract role from metadata if available
        String role;
        if (message.getMetadata() != null && message.getMetadata().containsKey("role")) {
            role = message.getMetadata().get("role").toString();
        } else {
            // FIXME, Infer role from message type?
            role = "user";
        }

        if (role != null) {
            msgObj.put("role", role);
        }

        // Extract content text
        String contentText = getQueryText(message);
        if (contentText != null && !contentText.isEmpty()) {
            msgObj.put("content", contentText);
        }

        return msgObj;
    }


    /**
     * Extract query text from message
     */
    private String getQueryText(Message message) {
        if (message == null || message.getContent() == null) {
            return "";
        }

        // Todo: TEST ME
        return message.getContent().stream()
                .filter(content -> "text".equals(content.getType()))
                .filter(content -> content instanceof TextContent)
                .map(content -> ((TextContent) content).getText())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    /**
     * Parse search response from Mem0 API
     */
    private List<Message> parseSearchResponse(String responseBody) {
        try {
            JsonNode results = OBJECT_MAPPER.readTree(responseBody);
            if (!results.isArray()) {
                return Collections.emptyList();
            }

            List<Message> messages = new ArrayList<>();
            for (JsonNode result : results) {
                if (result.has("memory")) {
                    String memoryText = result.get("memory").asText();
                    Message message = createMessageFromMemoryText(memoryText, result);
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            logger.severe("Failed to parse search response: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parse list response from Mem0 API
     */
    private List<Message> parseListResponse(String responseBody) {
        try {
            JsonNode results = OBJECT_MAPPER.readTree(responseBody);
            if (!results.isArray()) {
                return Collections.emptyList();
            }

            List<Message> messages = new ArrayList<>();
            for (JsonNode result : results) {
                if (result.has("memory")) {
                    String memoryText = result.get("memory").asText();
                    Message message = createMessageFromMemoryText(memoryText, result);
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            logger.severe("Failed to parse list response: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Create Message from memory text and metadata
     */
    private Message createMessageFromMemoryText(String memoryText, JsonNode memoryNode) {
        Message message = new Message();
        // Todo: TEST ME
        message.setType(MessageType.MESSAGE);
        message.setRole(Role.USER);

        Content content = new TextContent(memoryText);
        message.setContent(Collections.singletonList(content));

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        if (memoryNode.has("id")) {
            metadata.put("memory_id", memoryNode.get("id").asText());
        }
        if (memoryNode.has("created_at")) {
            metadata.put("created_at", memoryNode.get("created_at").asText());
        }
        if (memoryNode.has("updated_at")) {
            metadata.put("updated_at", memoryNode.get("updated_at").asText());
        }
        if (memoryNode.has("metadata") && memoryNode.get("metadata").isObject()) {
            JsonNode metadataNode = memoryNode.get("metadata");
            metadataNode.fields().forEachRemaining(entry ->
                metadata.put(entry.getKey(), entry.getValue().asText())
            );
        }

        message.setMetadata(metadata);
        return message;
    }
}

