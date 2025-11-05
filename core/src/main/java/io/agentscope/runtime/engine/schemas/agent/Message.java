package io.agentscope.runtime.engine.schemas.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.agentscope.runtime.engine.memory.model.MessageType;

public class Message extends Event {
    
    private String id;
    private String type = MessageType.MESSAGE.name();
    private String role;
    private List<Content> content;
    private String code;
    private String message;
    private Map<String, Object> usage;
    
    public Message() {
        super();
        this.id = "msg_" + UUID.randomUUID().toString();
        this.object = "message";
        this.status = RunStatus.CREATED;
        this.content = new ArrayList<>();
    }
    
    public Message(String type, String role) {
        this();
        this.type = type;
        this.role = role;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
        this.content = content != null ? content : new ArrayList<>();
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
    
    public static Message fromOpenAiMessage(Map<String, Object> message) {
        Message msg = new Message();
        
        String role = (String) message.get("role");
        msg.setRole(role);
        
        Object contentObj = message.get("content");
        if (contentObj instanceof String) {
            TextContent textContent = new TextContent((String) contentObj);
            msg.setContent(List.of(textContent));
        } else if (contentObj instanceof List) {
            List<Content> contents = new ArrayList<>();
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;
            for (Map<String, Object> contentItem : contentList) {
                String contentType = (String) contentItem.get("type");
                if ("text".equals(contentType)) {
                    TextContent textContent = new TextContent((String) contentItem.get("text"));
                    contents.add(textContent);
                } else if ("image_url".equals(contentType)) {
                    Map<String, Object> imageUrl = (Map<String, Object>) contentItem.get("image_url");
                    ImageContent imageContent = new ImageContent((String) imageUrl.get("url"));
                    contents.add(imageContent);
                }
            }
            msg.setContent(contents);
        }
        
        if ("assistant".equals(role) && message.containsKey("tool_calls")) {
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            List<Content> toolContents = new ArrayList<>();
            for (Map<String, Object> toolCall : toolCalls) {
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                FunctionCall functionCall = new FunctionCall(
                    (String) toolCall.get("id"),
                    (String) function.get("name"),
                    (String) function.get("arguments")
                );
                DataContent dataContent = new DataContent(functionCall.toMap());
                toolContents.add(dataContent);
            }
            msg.setType(MessageType.FUNCTION_CALL.name());
            msg.setContent(toolContents);
        }
        
        if ("tool".equals(role)) {
            FunctionCallOutput functionCallOutput = new FunctionCallOutput(
                (String) message.get("tool_call_id"),
                (String) message.get("content")
            );
            DataContent dataContent = new DataContent(functionCallOutput.toMap());
            msg.setType(MessageType.FUNCTION_CALL_OUTPUT.name());
            msg.setContent(List.of(dataContent));
        }
        
        return msg;
    }
    
    public String getTextContent() {
        if (content == null) {
            return null;
        }
        
        for (Content item : content) {
            if (item instanceof TextContent) {
                return ((TextContent) item).getText();
            }
        }
        return null;
    }
    
    public List<String> getImageContent() {
        List<String> images = new ArrayList<>();
        
        if (content == null) {
            return images;
        }
        
        for (Content item : content) {
            if (item instanceof ImageContent) {
                images.add(((ImageContent) item).getImageUrl());
            }
        }
        return images;
    }
    
    public Content addDeltaContent(Content newContent) {
        if (content == null) {
            content = new ArrayList<>();
        }
        
        if (newContent.getIndex() == null) {
            Content copy = copyContent(newContent);
            copy.setDelta(null);
            copy.setIndex(null);
            copy.setMsgId(null);
            content.add(copy);
            
            newContent.setIndex(content.size() - 1);
            newContent.setMsgId(this.id);
            newContent.inProgress();
            return newContent;
        }
        
        if (Boolean.TRUE.equals(newContent.getDelta())) {
            Content preContent = content.get(newContent.getIndex());
            String type = preContent.getType();
            
            if (ContentType.TEXT.equals(type)) {
                TextContent preText = (TextContent) preContent;
                TextContent newText = (TextContent) newContent;
                preText.setText(preText.getText() + newText.getText());
            }
            
            if (ContentType.IMAGE.equals(type)) {
                ImageContent preImage = (ImageContent) preContent;
                ImageContent newImage = (ImageContent) newContent;
                preImage.setImageUrl(preImage.getImageUrl() + newImage.getImageUrl());
            }
            
            if (ContentType.DATA.equals(type)) {
                DataContent preData = (DataContent) preContent;
                DataContent newData = (DataContent) newContent;
            }
            
            newContent.setMsgId(this.id);
            newContent.inProgress();
            return newContent;
        }
        
        return null;
    }
    
    public Content contentCompleted(int contentIndex) {
        if (content == null || contentIndex >= content.size()) {
            return null;
        }
        
        Content contentItem = content.get(contentIndex);
        Content newContent = copyContent(contentItem);
        newContent.setDelta(false);
        newContent.setIndex(contentIndex);
        newContent.setMsgId(this.id);
        newContent.completed();
        return newContent;
    }
    
    public Content addContent(Content newContent) {
        if (content == null) {
            content = new ArrayList<>();
        }
        
        if (newContent.getIndex() == null) {
            Content copy = copyContent(newContent);
            content.add(copy);
            
            newContent.setIndex(content.size() - 1);
            newContent.setMsgId(this.id);
            newContent.completed();
            return newContent;
        }
        
        return null;
    }
    
    private Content copyContent(Content original) {
        if (original instanceof TextContent) {
            TextContent text = (TextContent) original;
            return new TextContent(text.getText());
        } else if (original instanceof ImageContent) {
            ImageContent image = (ImageContent) original;
            return new ImageContent(image.getImageUrl());
        } else if (original instanceof DataContent) {
            DataContent data = (DataContent) original;
            return new DataContent(data.getData());
        }
        return original;
    }
}
