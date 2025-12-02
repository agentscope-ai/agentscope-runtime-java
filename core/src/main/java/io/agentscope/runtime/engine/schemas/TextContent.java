package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Text content in a message.
 */
public class TextContent extends Content {
    @JsonProperty("text")
    private String text;
    
    public TextContent() {
        this.setType(ContentType.TEXT);
    }
    
    public TextContent(String text) {
        this();
        this.text = text;
    }
    
    public TextContent(Boolean delta, Integer index, String text) {
        this(text);
        this.setDelta(delta);
        this.setIndex(index);
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}

