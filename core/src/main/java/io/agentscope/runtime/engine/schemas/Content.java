package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for content in messages.
 */
public class Content extends Event {
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("object")
    private String object = "content";
    
    @JsonProperty("index")
    private Integer index;
    
    @JsonProperty("delta")
    private Boolean delta;
    
    @JsonProperty("msg_id")
    private String msgId;
    
    public Content() {
        super();
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getObject() {
        return object;
    }
    
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
    
    public Content completed() {
        this.delta = false;
        super.completed();
        return this;
    }
    
    public Content inProgress() {
        super.inProgress();
        return this;
    }
}

