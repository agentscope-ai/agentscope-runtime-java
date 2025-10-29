package io.agentscope.chatbot.controller;

import io.agentscope.chatbot.model.Conversation;
import io.agentscope.chatbot.model.Message;
import io.agentscope.chatbot.model.User;
import io.agentscope.chatbot.repository.ConversationRepository;
import io.agentscope.chatbot.repository.MessageRepository;
import io.agentscope.chatbot.repository.UserRepository;
import io.agentscope.chatbot.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Web Server REST Controller
 * Equivalent to web_server.py
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WebServerController {

    private static final Logger logger = LoggerFactory.getLogger(WebServerController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AgentService agentService;

    /**
     * User login
     * POST /api/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Username and password cannot be empty"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.checkPassword(password)) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("name", user.getName());
                response.put("created_at", user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password"));
    }

    /**
     * Get all user conversations
     * GET /api/users/{user_id}/conversations
     */
    @GetMapping("/users/{userId}/conversations")
    public ResponseEntity<?> getUserConversations(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation conv : conversations) {
            Map<String, Object> convData = new HashMap<>();
            convData.put("id", conv.getId());
            convData.put("title", conv.getTitle());
            convData.put("user_id", conv.getUser().getId());

            // Get last message preview
            Optional<Message> lastMessage = messageRepository
                    .findFirstByConversationIdOrderByCreatedAtDesc(conv.getId());
            String preview = lastMessage.map(Message::getText).orElse("");
            convData.put("preview", preview);

            convData.put("created_at", conv.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
            convData.put("updated_at", conv.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

            result.add(convData);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Create new conversation
     * POST /api/users/{user_id}/conversations
     */
    @PostMapping("/users/{userId}/conversations")
    public ResponseEntity<?> createConversation(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> request) {

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        String title = null;
        if (request != null) {
            title = request.get("title");
        }

        if (title == null || title.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            title = "Conversation " + LocalDateTime.now().format(formatter);
        }

        Conversation conversation = new Conversation();
        conversation.setTitle(title);
        conversation.setUser(user);
        conversation = conversationRepository.save(conversation);

        // Create welcome message
        Message welcomeMessage = new Message();
        welcomeMessage.setText("Hello! I am your AI assistant. How can I help you today?");
        welcomeMessage.setSender("ai");
        welcomeMessage.setConversation(conversation);
        messageRepository.save(welcomeMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("id", conversation.getId());
        response.put("title", conversation.getTitle());
        response.put("user_id", conversation.getUser().getId());
        response.put("created_at", conversation.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("updated_at", conversation.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get conversation details and messages
     * GET /api/conversations/{conversation_id}
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(@PathVariable Long conversationId) {
        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (!convOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Conversation not found"));
        }

        Conversation conversation = convOpt.get();
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Map<String, Object>> messagesData = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> msgData = new HashMap<>();
            msgData.put("id", msg.getId());
            msgData.put("text", msg.getText());
            msgData.put("sender", msg.getSender());
            msgData.put("created_at", msg.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
            messagesData.add(msgData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", conversation.getId());
        response.put("title", conversation.getTitle());
        response.put("user_id", conversation.getUser().getId());
        response.put("messages", messagesData);
        response.put("created_at", conversation.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("updated_at", conversation.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    /**
     * Send message
     * POST /api/conversations/{conversation_id}/messages
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> request) {

        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (!convOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Conversation not found"));
        }

        Conversation conversation = convOpt.get();

        String text = request.get("text");
        String sender = request.getOrDefault("sender", "user");

        if (text == null || text.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Message content cannot be empty"));
        }

        // Create user message
        Message userMessage = new Message();
        userMessage.setText(text);
        userMessage.setSender(sender);
        userMessage.setConversation(conversation);
        userMessage = messageRepository.save(userMessage);

        // Update conversation title if it's the first user message
        if ("user".equals(sender)) {
            List<Message> allMessages = conversation.getMessages();
            if (allMessages.size() <= 2) {  // Welcome message + this message
                String newTitle = text.length() > 20 ? text.substring(0, 20) + "..." : text;
                conversation.setTitle(newTitle);
                conversationRepository.save(conversation);
            }

            // Call agent to get AI response
            String conversationIdStr = String.valueOf(conversationId);
            String aiResponseText = agentService.callAgent(text, conversationIdStr, conversationIdStr);

            // Save AI message
            Message aiMessage = new Message();
            aiMessage.setText(aiResponseText);
            aiMessage.setSender("ai");
            aiMessage.setConversation(conversation);
            messageRepository.save(aiMessage);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", userMessage.getId());
        response.put("text", userMessage.getText());
        response.put("sender", userMessage.getSender());
        response.put("created_at", userMessage.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Delete conversation
     * DELETE /api/conversations/{conversation_id}
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long conversationId) {
        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (!convOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Conversation not found"));
        }

        conversationRepository.delete(convOpt.get());
        return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
    }

    /**
     * Update conversation title
     * PUT /api/conversations/{conversation_id}
     */
    @PutMapping("/conversations/{conversationId}")
    public ResponseEntity<?> updateConversation(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> request) {

        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (!convOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Conversation not found"));
        }

        Conversation conversation = convOpt.get();

        if (request.containsKey("title")) {
            conversation.setTitle(request.get("title"));
            conversation = conversationRepository.save(conversation);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", conversation.getId());
        response.put("title", conversation.getTitle());
        response.put("user_id", conversation.getUser().getId());
        response.put("created_at", conversation.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("updated_at", conversation.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    /**
     * Get user information
     * GET /api/users/{user_id}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("created_at", user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }
}

