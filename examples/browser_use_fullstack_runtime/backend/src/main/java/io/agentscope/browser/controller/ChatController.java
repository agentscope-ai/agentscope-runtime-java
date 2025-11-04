package io.agentscope.browser.controller;

import io.agentscope.browser.agent.AgentscopeBrowseruseAgent;
import io.agentscope.runtime.engine.schemas.agent.Content;
import io.agentscope.runtime.engine.schemas.agent.DataContent;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
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

    private final AgentscopeBrowseruseAgent agent;

    public ChatController(AgentscopeBrowseruseAgent agent) {
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
                    if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
                        return Flux.just(simpleYield("", "content"));
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
                        return Flux.just(simpleYield(response, "content"));
                    } else {
                        return Flux.just(simpleYield("", "content"));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error during chat completion", error);
                    return Flux.just(simpleYield("Error: " + error.getMessage(), "content"));
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
                    if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
                        return Flux.just(simpleYield("", "content"));
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
                        return Flux.just(simpleYield(response, "content"));
                    } else {
                        return Flux.just(simpleYield("", "content"));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error during chat completion", error);
                    return Flux.just(simpleYield("Error: " + error.getMessage(), "content"));
                });
    }


    /**
     * Get browser environment info
     */
    @GetMapping("/env_info")
    public ResponseEntity<Map<String, String>> getEnvInfo() {
        logger.info("Received env_info request");

        String wsUrl = agent.getBrowserWebSocketUrl();

        if (wsUrl != null && !wsUrl.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("url", wsUrl);
            logger.info("Returning WebSocket URL: {}", wsUrl);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "WebSocket connection failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Format response as SSE (Server-Sent Events) compatible string
     */
    private String simpleYield(String content, String ctype) {
        Map<String, Object> response = wrapAsOpenAIResponse(content, content, ctype);

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
    private Map<String, Object> wrapAsOpenAIResponse(String textContent, String cardContent, String ctype) {
        String contentType;

        contentType = switch (ctype) {
            case "think" -> "reasoning_content";
            case "site" -> "site_content";
            default -> "content";
        };

        Map<String, Object> delta = new HashMap<>();
        delta.put(contentType, textContent);
        delta.put("cards", cardContent);

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
}

