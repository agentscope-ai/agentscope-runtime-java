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

package io.agentscope.browser.controller;

import io.agentscope.browser.agent.AgentscopeBrowserUseAgent;
import io.agentscope.runtime.engine.schemas.message.MessageType;
import io.agentscope.runtime.engine.schemas.message.Content;
import io.agentscope.runtime.engine.schemas.message.DataContent;
import io.agentscope.runtime.engine.schemas.message.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for browser agent chat endpoints
 */
@RestController
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final AgentscopeBrowserUseAgent agent;

    public ChatController(AgentscopeBrowserUseAgent agent) {
        this.agent = agent;
    }

    /**
     * Stream chat completions endpoint (OpenAI-compatible)
     */
    @PostMapping(value = {"/v1/chat/completions", "/chat/completions"},
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatCompletions(@RequestBody Map<String, Object> request) {
        logger.info("Received chat completion request");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");

        if (messages == null || messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No messages provided"));
        }



        return agent.chat(messages)
                .flatMap(message -> {
                    if (message == null) {
                        return Flux.just(simpleYield("", "content", null, null, null, null));
                    }

                    // Get message type
                    MessageType messageType = message.getType();

                    // If it's TOOL_CALL or TOOL_RESPONSE, extract tool information
                    if (messageType == MessageType.TOOL_CALL || messageType == MessageType.TOOL_RESPONSE) {
                        String toolName = null;
                        String toolId = null;
                        String toolInput = null;
                        String toolResult = null;

                        // Extract tool information from content
                        if (message.getContent() != null && !message.getContent().isEmpty()) {
                            for (Content item : message.getContent()) {
                                if (item instanceof TextContent textContent) {
                                    String text = textContent.getText();
                                    if (text != null && !text.isEmpty()) {
                                        Map<String, String> toolInfo = parseToolInfo(text);
                                        if (toolInfo != null) {
                                            toolName = (String)toolInfo.get("toolName");
                                            toolId = (String)toolInfo.get("toolId");
                                            if (messageType == MessageType.TOOL_CALL) {
                                                toolInput = toolInfo.get("toolInput").toString();
                                            } else {
                                                toolResult = toolInfo.get("toolResult").toString();
                                            }
                                        }
                                    }
                                } else if (item instanceof DataContent dataContent) {
                                    Map<String, Object> data = dataContent.getData();
                                    if (data != null) {
                                        toolName = data.containsKey("name") ? String.valueOf(data.get("name")) : null;
                                        toolId = data.containsKey("id") ? String.valueOf(data.get("id")) : null;
                                        if (messageType == MessageType.TOOL_CALL) {
                                            toolInput = data.containsKey("input") ? String.valueOf(data.get("input")) : null;
                                        } else {
                                            toolResult = data.containsKey("result") ? String.valueOf(data.get("result")) : null;
                                        }
                                    }
                                }
                            }
                        }

                        return Flux.just(simpleYield("", "content", messageType.name(), toolName, toolId,
                                messageType == MessageType.TOOL_CALL ? toolInput : toolResult));
                    }

                    if (message.getContent() == null || message.getContent().isEmpty()) {
                        return Flux.just(simpleYield("", "content", messageType != null ? messageType.name() : null, null, null, null));
                    }

                    List<Content> contentList = message.getContent();
                    StringBuilder responseBuilder = new StringBuilder();

                    for (Content item : contentList) {
                        if (item instanceof TextContent textContent) {
                            String text = textContent.getText();
                            if (text != null && !text.isEmpty()) {
                                responseBuilder.append(text);
                                System.out.println(text);
                            }
                        } else if (item instanceof DataContent dataContent) {
                            Map<String, Object> data = dataContent.getData();
                            if (data != null && data.containsKey("name")) {
                                String toolName = String.valueOf(data.get("name"));
                                responseBuilder.append("Using tool: ").append(toolName).append("\n");
                            }
                        }
                    }

                    String response = responseBuilder.toString();
                    if (!response.isEmpty()) {
                        return Flux.just(simpleYield(response, "content", messageType != null ? messageType.name() : null, null, null, null));
                    } else {
                        return Flux.just(simpleYield("", "content", messageType != null ? messageType.name() : null, null, null, null));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error during chat completion", error);
                    return Flux.just(simpleYield("Error: " + error.getMessage(), "content", null, null, null, null));
                });
    }


    @PostMapping(value = {"/v1/chatSimple/completions", "/chatSimple/completions"},
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatSimpleCompletions(String userMessage) {
        logger.info("Received chat completion request");

        if (userMessage == null || userMessage.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No messages provided"));
        }

        return agent.chatSimple(userMessage)
                .flatMap(message -> {
                    System.out.println("Received message: " + message);
                    if (message == null) {
                        return Flux.just(simpleYield("", "content", null, null, null, null));
                    }

                    // Get message type
                    MessageType messageType = message.getType();

                    if (messageType == MessageType.TOOL_CALL || messageType == MessageType.TOOL_RESPONSE) {
                        String toolName = null;
                        String toolId = null;
                        String toolInput = null;
                        String toolResult = null;

                        if (message.getContent() != null && !message.getContent().isEmpty()) {
                            for (Content item : message.getContent()) {
                                if (item instanceof TextContent textContent) {
                                    String text = textContent.getText();
                                    if (text != null && !text.isEmpty()) {
                                        Map<String, String> toolInfo = parseToolInfo(text);
                                        if (toolInfo != null) {
                                            toolName = toolInfo.get("toolName");
                                            toolId = toolInfo.get("toolId");
                                            if (messageType == MessageType.TOOL_CALL) {
                                                toolInput = toolInfo.get("toolInput");
                                            } else {
                                                toolResult = toolInfo.get("toolResult");
                                            }
                                        }
                                    }
                                } else if (item instanceof DataContent dataContent) {
                                    Map<String, Object> data = dataContent.getData();
                                    if (data != null) {
                                        toolName = data.containsKey("name") ? String.valueOf(data.get("name")) : null;
                                        toolId = data.containsKey("id") ? String.valueOf(data.get("id")) : null;
                                        if (messageType == MessageType.TOOL_CALL) {
                                            toolInput = "";
                                        } else {
                                            toolResult = "";
                                        }
                                    }
                                }
                            }
                        }

                        return Flux.just(simpleYield("", "content", messageType.name(), toolName, toolId,
                                messageType == MessageType.TOOL_CALL ? toolInput : toolResult));
                    }

                    if (message.getContent() == null || message.getContent().isEmpty()) {
                        return Flux.just(simpleYield("", "content", messageType != null ? messageType.name() : null, null, null, null));
                    }

                    List<Content> contentList = message.getContent();
                    StringBuilder responseBuilder = new StringBuilder();

                    for (Content item : contentList) {
                        if (item instanceof TextContent textContent) {
                            String text = textContent.getText();
                            if (text != null && !text.isEmpty()) {
                                responseBuilder.append(text);
                            }
                        } else if (item instanceof DataContent dataContent) {
                            Map<String, Object> data = dataContent.getData();
                            if (data != null && data.containsKey("name")) {
                                String toolName = String.valueOf(data.get("name"));
                                responseBuilder.append("Using tool: ").append(toolName).append("\n");
                            }
                        }
                    }

                    String response = responseBuilder.toString();
                    if (!response.isEmpty()) {
                        return Flux.just(simpleYield(response, "content", messageType != null ? messageType.name() : null, null, null, null));
                    } else {
                        return Flux.just(simpleYield("", "content", messageType != null ? messageType.name() : null, null, null, null));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error during chat completion", error);
                    return Flux.just(simpleYield("Error: " + error.getMessage(), "content", null, null, null, null));
                });
    }


    /**
     * Get browser environment info
     */
    @GetMapping("/env_info")
    public ResponseEntity<Map<String, String>> getEnvInfo() {
        logger.info("Received env_info request");

        String baseUrl = agent.getBaseUrl();
        String runtimeToken = agent.getRuntimeToken();

        if (baseUrl != null && !baseUrl.isEmpty() && runtimeToken != null && !runtimeToken.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("baseUrl", baseUrl);
            response.put("runtimeToken", runtimeToken);
            logger.info("Returning baseUrl: {}, runtimeToken: {}", baseUrl, runtimeToken);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Browser sandbox not available");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Format response as SSE (Server-Sent Events) compatible string
     */
    private String simpleYield(String content, String ctype, String messageType, String toolName, String toolId, String toolData) {
        Map<String, Object> response = wrapAsOpenAIResponse(content, content, ctype, messageType, toolName, toolId, toolData);

        try {
            // Use simple JSON formatting
            String json = toJson(response);
            return "data: " + json + "\n\n";
        } catch (Exception e) {
            logger.error("Error formatting response", e);
            return "data: {\"error\": \"" + e.getMessage() + "\"}\n\n";
        }
    }

    /**
     * Wrap content as OpenAI-compatible response
     */
    private Map<String, Object> wrapAsOpenAIResponse(String textContent, String cardContent, String ctype, String messageType, String toolName, String toolId, String toolData) {
        String contentType;

        contentType = switch (ctype) {
            case "think" -> "reasoning_content";
            case "site" -> "site_content";
            default -> "content";
        };

        Map<String, Object> delta = new HashMap<>();
        delta.put(contentType, textContent);
        delta.put("cards", cardContent);
        // Add messageType to delta if present
        if (messageType != null) {
            delta.put("messageType", messageType);
        }
        // Add tool information if present
        if (toolName != null) {
            delta.put("toolName", toolName);
        }
        if (toolId != null) {
            delta.put("toolId", toolId);
        }
        if (toolData != null) {
            if (MessageType.TOOL_CALL.name().equals(messageType)) {
                delta.put("toolInput", toolData);
            } else if (MessageType.TOOL_RESPONSE.name().equals(messageType)) {
                delta.put("toolResult", toolData);
            }
        }

        Map<String, Object> choice = new HashMap<>();
        choice.put("delta", delta);
        choice.put("index", 0);
        choice.put("finish_reason", null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", "chat_" + System.currentTimeMillis());
        response.put("object", "chat.completion.chunk");
        response.put("created", System.currentTimeMillis() / 1000);
        response.put("choices", List.of(choice));

        return response;
    }

    /**
     * Simple JSON serialization
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("\"").append(entry.getKey()).append("\":");
            json.append(toJsonValue(entry.getValue()));
        }

        json.append("}");
        return json.toString();
    }

    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append(toJsonValue(item));
            }
            json.append("]");
            return json.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return toJson(map);
        }
        return "\"" + value.toString() + "\"";
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Parse tool information from Map string format: {toolName=..., toolId=..., toolInput=...} or {toolName=..., toolId=..., result=...}
     */
    private Map<String, String> parseToolInfo(String mapString) {
        if (mapString == null || !mapString.startsWith("{") || !mapString.endsWith("}")) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        try {
            // Remove outer braces
            String content = mapString.substring(1, mapString.length() - 1).trim();
            if (content.isEmpty()) {
                return result;
            }

            // Split by comma, but be careful with commas inside values
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse tool info from string: " + mapString, e);
            return null;
        }

        return result;
    }
}

