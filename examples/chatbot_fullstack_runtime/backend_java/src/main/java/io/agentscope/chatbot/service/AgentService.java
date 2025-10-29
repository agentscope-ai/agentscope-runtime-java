package io.agentscope.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Service to call the Agent Runner
 */
@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final WebClient webClient;
    private final String agentUrl;

    public AgentService() {
        String serverHost = System.getenv("SERVER_HOST");
        if (serverHost == null || serverHost.isEmpty()) {
            serverHost = "localhost";
        }

        String serverPort = System.getenv("SERVER_PORT");
        if (serverPort == null || serverPort.isEmpty()) {
            serverPort = "8090";
        }

        String serverEndpoint = System.getenv("SERVER_ENDPOINT");
        if (serverEndpoint == null || serverEndpoint.isEmpty()) {
            serverEndpoint = "agent";
        }

        this.agentUrl = String.format("http://%s:%s/%s", serverHost, serverPort, serverEndpoint);
        this.webClient = WebClient.builder().build();

        logger.info("Agent service URL: {}", agentUrl);
    }

    /**
     * Call agent and get streaming response
     */
    public String callAgent(String query, String userId, String sessionId) {
        logger.info("Calling agent for user {} session {}", userId, sessionId);

        Map<String, Object> requestBody = Map.of(
            "input", List.of(
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of(
                            "type", "text",
                            "text", query
                        )
                    )
                )
            ),
            "session_id", sessionId,
            "user_id", userId
        );

        StringBuilder response = new StringBuilder();

        try {
            Flux<String> flux = webClient.post()
                    .uri(agentUrl)
                    .header("Accept", "text/event-stream")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class);

            flux.toStream().forEach(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    try {
                        // Simple JSON parsing for content
                        if (data.contains("\"object\":\"content\"") &&
                            data.contains("\"delta\":true") &&
                            data.contains("\"type\":\"text\"")) {
                            // Extract text field
                            int textStart = data.indexOf("\"text\":\"");
                            if (textStart >= 0) {
                                textStart += 8;
                                int textEnd = data.indexOf("\"", textStart);
                                if (textEnd > textStart) {
                                    String text = data.substring(textStart, textEnd);
                                    // Unescape JSON string
                                    text = text.replace("\\n", "\n")
                                              .replace("\\\"", "\"")
                                              .replace("\\\\", "\\");
                                    response.append(text);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Error parsing SSE data: {}", e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Error calling agent service", e);
            return "Sorry, there was an error processing your request.";
        }

        return response.toString();
    }
}

