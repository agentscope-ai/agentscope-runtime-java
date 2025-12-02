package io.agentscope.runtime.engine.helpers;

import io.agentscope.runtime.engine.schemas.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Response Builder - matches Python version's ResponseBuilder class.
 * Responsible for building and managing AgentResponse objects,
 * coordinating MessageBuilder work.
 */
public class ResponseBuilder {
    private String sessionId;
    private String responseId;
    private Long createdAt;
    private List<MessageBuilder> messageBuilders;
    private AgentResponse response;
    
    /**
     * Initialize Response Builder.
     */
    public ResponseBuilder() {
        this(null, null);
    }
    
    public ResponseBuilder(String sessionId) {
        this(sessionId, null);
    }
    
    public ResponseBuilder(String sessionId, String responseId) {
        this.sessionId = sessionId;
        this.responseId = responseId;
        this.createdAt = System.currentTimeMillis() / 1000; // Unix timestamp
        this.messageBuilders = new ArrayList<>();
        
        // Create response object
        this.response = new AgentResponse(
            this.responseId,
            this.sessionId,
            this.createdAt
        );
        this.response.setOutput(new ArrayList<>());
    }
    
    /**
     * Reset builder state, generate new ID and object instances.
     */
    public void reset() {
        this.responseId = "response_" + UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis() / 1000;
        this.messageBuilders = new ArrayList<>();
        
        // Recreate response object
        this.response = new AgentResponse(
            this.responseId,
            this.sessionId,
            this.createdAt
        );
        this.response.setOutput(new ArrayList<>());
    }
    
    /**
     * Get dictionary representation of current response.
     */
    public AgentResponse getResponseData() {
        return response;
    }
    
    /**
     * Set response status to created.
     */
    public AgentResponse created() {
        response.created();
        return response;
    }
    
    /**
     * Set response status to in_progress.
     */
    public AgentResponse inProgress() {
        response.inProgress();
        return response;
    }
    
    /**
     * Set response status to completed.
     */
    public AgentResponse completed() {
        response.completed();
        return response;
    }
    
    /**
     * Create Message Builder.
     * 
     * @param role Message role, defaults to assistant
     * @param messageType Message type, defaults to message
     */
    public MessageBuilder createMessageBuilder(String role, String messageType) {
        if (role == null) {
            role = Role.ASSISTANT;
        }
        if (messageType == null) {
            messageType = MessageType.MESSAGE;
        }
        
        MessageBuilder messageBuilder = new MessageBuilder(this, role, messageType);
        this.messageBuilders.add(messageBuilder);
        return messageBuilder;
    }
    
    /**
     * Add message to response output list.
     */
    public void addMessage(Message message) {
        if (response.getOutput() == null) {
            response.setOutput(new ArrayList<>());
        }
        
        // Check if message with same ID already exists, replace if exists
        List<Message> output = response.getOutput();
        for (int i = 0; i < output.size(); i++) {
            if (output.get(i).getId().equals(message.getId())) {
                output.set(i, message);
                return;
            }
        }
        
        output.add(message);
    }
    
    /**
     * Update message in response.
     */
    public void updateMessage(Message message) {
        if (response.getOutput() == null) {
            return;
        }
        
        List<Message> output = response.getOutput();
        for (int i = 0; i < output.size(); i++) {
            if (output.get(i).getId().equals(message.getId())) {
                output.set(i, message);
                return;
            }
        }
    }
    
    /**
     * Generate complete streaming response sequence.
     * Matches Python's generate_streaming_response method.
     * 
     * @param textTokens Text fragment list
     * @param role Message role, defaults to assistant
     * @return Stream of response objects generated in order
     */
    public Stream<Event> generateStreamingResponse(List<String> textTokens, String role) {
        if (role == null) {
            role = Role.ASSISTANT;
        }
        
        List<Event> events = new ArrayList<>();
        
        // Reset state
        reset();
        
        // 1. Create response (created)
        events.add(created());
        
        // 2. Start response (in_progress)
        events.add(inProgress());
        
        // 3. Create Message Builder
        MessageBuilder messageBuilder = createMessageBuilder(role, MessageType.MESSAGE);
        events.add(messageBuilder.getMessageData());
        
        // 4. Create Content Builder
        ContentBuilder contentBuilder = messageBuilder.createContentBuilder(ContentType.TEXT);
        
        // 5. Stream output Text fragments
        for (String token : textTokens) {
            events.add(contentBuilder.addTextDelta(token));
        }
        
        // 6. Complete content
        events.add(contentBuilder.complete());
        
        // 7. Complete message
        events.add(messageBuilder.complete());
        
        // 8. Complete response
        events.add(completed());
        
        return events.stream();
    }
    
    /**
     * Message Builder - matches Python version's MessageBuilder class.
     * Responsible for building and managing individual Message objects
     * and updating associated Response.
     */
    public class MessageBuilder {
        private final ResponseBuilder responseBuilder;
        private final String role;
        private final String messageId;
        private final List<ContentBuilder> contentBuilders;
        private final Message message;
        
        public MessageBuilder(ResponseBuilder responseBuilder, String role, String messageType) {
            this.responseBuilder = responseBuilder;
            this.role = role;
            this.messageId = "msg_" + UUID.randomUUID().toString();
            this.contentBuilders = new ArrayList<>();
            
            // Create message object
            this.message = new Message(messageType, role);
            this.message.setId(messageId);
            this.message.inProgress();
            
            // Immediately add to response output
            responseBuilder.addMessage(this.message);
        }
        
        /**
         * Create Content Builder.
         * 
         * @param contentType Content type ('text', 'image', 'data')
         */
        public ContentBuilder createContentBuilder(String contentType) {
            if (contentType == null) {
                contentType = ContentType.TEXT;
            }
            
            int index = contentBuilders.size();
            ContentBuilder contentBuilder = new ContentBuilder(this, contentType, index);
            contentBuilders.add(contentBuilder);
            return contentBuilder;
        }
        
        /**
         * Add content to message.
         */
        public void addContent(Content content) {
            if (message.getContent() == null) {
                message.setContent(new ArrayList<>());
            }
            
            List<Content> messageContent = message.getContent();
            
            // Check if content with same index already exists, replace if exists
            Integer contentIndex = content.getIndex();
            if (contentIndex != null) {
                for (int i = 0; i < messageContent.size(); i++) {
                    Content existing = messageContent.get(i);
                    if (contentIndex.equals(existing.getIndex())) {
                        messageContent.set(i, content);
                        // Notify response builder to update
                        responseBuilder.updateMessage(message);
                        return;
                    }
                }
            }
            
            messageContent.add(content);
            // Notify response builder to update
            responseBuilder.updateMessage(message);
        }
        
        /**
         * Get dictionary representation of current message.
         */
        public Message getMessageData() {
            return message;
        }
        
        /**
         * Complete message building.
         */
        public Message complete() {
            message.completed();
            // Notify response builder to update
            responseBuilder.updateMessage(message);
            return message;
        }
    }
    
    /**
     * Content Builder - matches Python version's ContentBuilder class.
     * Responsible for building and managing individual Content objects,
     * supporting Text, Image, and Data content types.
     */
    public class ContentBuilder {
        private final MessageBuilder messageBuilder;
        private final String contentType;
        private final int index;
        private final Content content;
        private List<String> textTokens;
        private List<Map<String, Object>> dataDeltas;
        
        public ContentBuilder(MessageBuilder messageBuilder, String contentType, int index) {
            this.messageBuilder = messageBuilder;
            this.contentType = contentType;
            this.index = index;
            
            // Initialize corresponding data structures and content objects
            if (ContentType.TEXT.equals(contentType)) {
                this.textTokens = new ArrayList<>();
                this.content = new TextContent();
            } else if (ContentType.IMAGE.equals(contentType)) {
                this.content = new ImageContent();
            } else if (ContentType.DATA.equals(contentType)) {
                this.dataDeltas = new ArrayList<>();
                this.content = new DataContent();
            } else if (ContentType.AUDIO.equals(contentType)) {
                this.content = new AudioContent();
            } else {
                throw new IllegalArgumentException("Unsupported content type: " + contentType);
            }
            
            this.content.setType(contentType);
            this.content.setIndex(index);
            this.content.setMsgId(messageBuilder.messageId);
            this.content.setDelta(false);
        }
        
        /**
         * Add text delta (only applicable to text type).
         */
        public TextContent addTextDelta(String text) {
            if (!ContentType.TEXT.equals(contentType)) {
                throw new IllegalArgumentException("addTextDelta only supported for text content");
            }
            
            textTokens.add(text);
            
            // Create delta content
            TextContent deltaContent = new TextContent();
            deltaContent.setType(ContentType.TEXT);
            deltaContent.setIndex(index);
            deltaContent.setDelta(true);
            deltaContent.setMsgId(messageBuilder.messageId);
            deltaContent.setText(text);
            deltaContent.inProgress();
            
            return deltaContent;
        }
        
        /**
         * Set complete text content (only applicable to text type).
         */
        public TextContent setText(String text) {
            if (!ContentType.TEXT.equals(contentType)) {
                throw new IllegalArgumentException("setText only supported for text content");
            }
            
            if (content instanceof TextContent) {
                ((TextContent) content).setText(text);
                content.inProgress();
            }
            return (TextContent) content;
        }
        
        /**
         * Set image URL (only applicable to image type).
         */
        public ImageContent setImageUrl(String imageUrl) {
            if (!ContentType.IMAGE.equals(contentType)) {
                throw new IllegalArgumentException("setImageUrl only supported for image content");
            }
            
            if (content instanceof ImageContent) {
                ((ImageContent) content).setImageUrl(imageUrl);
                content.inProgress();
            }
            return (ImageContent) content;
        }
        
        /**
         * Set data content (only applicable to data type).
         */
        public DataContent setData(Map<String, Object> data) {
            if (!ContentType.DATA.equals(contentType)) {
                throw new IllegalArgumentException("setData only supported for data content");
            }
            
            if (content instanceof DataContent) {
                ((DataContent) content).setData(data);
                content.inProgress();
            }
            return (DataContent) content;
        }
        
        /**
         * Add data delta (only applicable to data type).
         */
        public DataContent addDataDelta(Map<String, Object> deltaData) {
            if (!ContentType.DATA.equals(contentType)) {
                throw new IllegalArgumentException("addDataDelta only supported for data content");
            }
            
            dataDeltas.add(deltaData);
            
            // Create delta content object
            DataContent deltaContent = new DataContent();
            deltaContent.setType(ContentType.DATA);
            deltaContent.setIndex(index);
            deltaContent.setDelta(true);
            deltaContent.setMsgId(messageBuilder.messageId);
            deltaContent.setData(deltaData);
            deltaContent.inProgress();
            
            return deltaContent;
        }
        
        /**
         * Complete content building.
         */
        public Message complete() {
            if (ContentType.TEXT.equals(contentType)) {
                // For text content, merge set text and tokens
                if (textTokens != null && !textTokens.isEmpty()) {
                    String existingText = content instanceof TextContent ? 
                        ((TextContent) content).getText() : "";
                    if (existingText == null) {
                        existingText = "";
                    }
                    String tokenText = String.join("", textTokens);
                    ((TextContent) content).setText(existingText + tokenText);
                }
                content.setDelta(false);
            } else if (ContentType.DATA.equals(contentType)) {
                // For data content, merge set data and delta data
                if (dataDeltas != null && !dataDeltas.isEmpty()) {
                    Map<String, Object> existingData = content instanceof DataContent ?
                        ((DataContent) content).getData() : null;
                    if (existingData == null) {
                        existingData = new HashMap<>();
                    }
                    
                    // Gradually merge all delta data
                    Map<String, Object> finalData = mergeDataIncrementally(existingData, dataDeltas);
                    ((DataContent) content).setData(finalData);
                }
                content.setDelta(false);
            }
            
            // Set completion status
            content.completed();
            
            // Update message content list
            messageBuilder.addContent(content);
            
            return messageBuilder.message;
        }
        
        /**
         * Intelligently merge data deltas.
         */
        private Map<String, Object> mergeDataIncrementally(
            Map<String, Object> baseData,
            List<Map<String, Object>> deltaList
        ) {
            Map<String, Object> result = baseData != null ? 
                new HashMap<>(baseData) : new HashMap<>();
            
            for (Map<String, Object> deltaData : deltaList) {
                for (Map.Entry<String, Object> entry : deltaData.entrySet()) {
                    String key = entry.getKey();
                    Object deltaValue = entry.getValue();
                    
                    if (!result.containsKey(key)) {
                        // New key, add directly
                        result.put(key, deltaValue);
                    } else {
                        Object baseValue = result.get(key);
                        // Perform delta merge based on data type
                        if (baseValue instanceof String && deltaValue instanceof String) {
                            // String concatenation
                            result.put(key, (String) baseValue + (String) deltaValue);
                        } else if (baseValue instanceof Number && deltaValue instanceof Number &&
                                   !(baseValue instanceof Boolean) && !(deltaValue instanceof Boolean)) {
                            // Numeric accumulation
                            if (baseValue instanceof Double || deltaValue instanceof Double) {
                                result.put(key, ((Number) baseValue).doubleValue() + 
                                    ((Number) deltaValue).doubleValue());
                            } else {
                                result.put(key, ((Number) baseValue).longValue() + 
                                    ((Number) deltaValue).longValue());
                            }
                        } else if (baseValue instanceof List && deltaValue instanceof List) {
                            // List merging
                            @SuppressWarnings("unchecked")
                            List<Object> merged = new ArrayList<>((List<Object>) baseValue);
                            merged.addAll((List<Object>) deltaValue);
                            result.put(key, merged);
                        } else if (baseValue instanceof Map && deltaValue instanceof Map) {
                            // Dictionary recursive merging
                            @SuppressWarnings("unchecked")
                            Map<String, Object> merged = mergeDataIncrementally(
                                (Map<String, Object>) baseValue,
                                List.of((Map<String, Object>) deltaValue)
                            );
                            result.put(key, merged);
                        } else {
                            // Other cases directly replace
                            result.put(key, deltaValue);
                        }
                    }
                }
            }
            
            return result;
        }
        
        /**
         * Get dictionary representation of current content.
         */
        public Content getContentData() {
            return content;
        }
        
        /**
         * Add text delta (backward compatibility method).
         */
        public TextContent addDelta(String text) {
            return addTextDelta(text);
        }
    }
}
