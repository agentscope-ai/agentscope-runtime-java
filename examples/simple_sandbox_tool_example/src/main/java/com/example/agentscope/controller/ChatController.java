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

package com.example.agentscope.controller;

import com.example.agentscope.model.ChatRequest;
import com.example.agentscope.model.ChatResponse;
import com.example.agentscope.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chat API controller
 * Provide external REST API endpoints
 */
@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AgentScope service is running ✓");
    }

    /**
     * Send a message to the Agent
     *
     * @param request Chat request
     * @return Chat response
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.chat(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("An error occurred while processing the request: " + e.getMessage());
            errorResponse.setError(e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get available tool list
     */
    @GetMapping("/tools")
    public ResponseEntity<?> getTools() {
        try {
            return ResponseEntity.ok(chatService.getAvailableTools());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to get tool list: " + e.getMessage());
        }
    }

    /**
     * Reset conversation history
     */
    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        try {
            chatService.resetMemory();
            return ResponseEntity.ok("Conversation history reset ✓");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Reset failed: " + e.getMessage());
        }
    }
}

