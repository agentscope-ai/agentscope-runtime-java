package io.agentscope.runtime.engine.schemas.agent;

import java.util.Map;

/**
 * Content base class
 * Corresponds to the Content class in agent_schemas.py of the Python version
 */
public abstract class Content extends Event {
    
    private String type;
    private String object = "content";
    private Integer index;
    private Boolean delta = false;
    private String msgId;
    
    public Content() {
        super();
    }
    
    public Content(String type) {
        super();
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public String getObject() {
        return object;
    }
    
    @Override
    public void setObject(String object) {
        this.object = object;
    }
    
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    public Boolean getDelta() {
        return delta;
    }
    
    public void setDelta(Boolean delta) {
        this.delta = delta;
    }
    
    public String getMsgId() {
        return msgId;
    }
    
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
    
    /**
     * Create content from chat completion chunk
     * Corresponds to the from_chat_completion_chunk method of the Python version
     */
    public static Content fromChatCompletionChunk(Map<String, Object> chunk, Integer index) {
        // Implementation needs to be based on the specific chat completion chunk format
        // Temporarily return null, needs to be implemented according to actual requirements
        return null;
    }
}
