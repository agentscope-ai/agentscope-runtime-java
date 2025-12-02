package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a message in the agent runtime.
 */
public class Message extends Event {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("object")
    private String object = "message";
    
    @JsonProperty("type")
    private String type = MessageType.MESSAGE;
    
    @JsonProperty("role")
    private String role;
    
    @JsonProperty("content")
    private List<Content> content;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("usage")
    private Map<String, Object> usage;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    public Message() {
        this.id = "msg_" + UUID.randomUUID().toString();
        this.content = new ArrayList<>();
        this.status = RunStatus.CREATED;
    }
    
    public Message(String type, String role) {
        this();
        this.type = type;
        this.role = role;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public List<Content> getContent() {
        return content;
    }
    
    public void setContent(List<Content> content) {
        this.content = content;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Adds delta content to the message.
     */
    public Content addDeltaContent(Content newContent) {
        if (content == null) {
            content = new ArrayList<>();
        }
        
        // Find existing content with same index or add new
        int index = newContent.getIndex() != null ? newContent.getIndex() : content.size();
        newContent.setIndex(index);
        
        if (index < content.size()) {
            // Update existing content
            Content existing = content.get(index);
            if (existing.getType().equals(newContent.getType())) {
                // Merge content
                if (newContent instanceof TextContent && existing instanceof TextContent) {
                    TextContent textContent = (TextContent) existing;
                    TextContent newTextContent = (TextContent) newContent;
                    textContent.setText(textContent.getText() + newTextContent.getText());
                }
                return existing;
            }
        }
        
        // Add new content
        content.add(newContent);
        return newContent;
    }
    
    /**
     * Marks the message as in progress.
     */
    public Message inProgress() {
        this.status = RunStatus.IN_PROGRESS;
        return this;
    }
    
    /**
     * Marks the message as completed.
     */
    public Message completed() {
        this.status = RunStatus.COMPLETED;
        return this;
    }
}

