package io.agentscope.runtime.engine.schemas.agent;

/**
 * Text content class
 * Corresponds to the TextContent class in agent_schemas.py of the Python version
 */
public class TextContent extends Content {
    
    private String text;
    
    public TextContent() {
        super(ContentType.TEXT);
    }
    
    public TextContent(String text) {
        super(ContentType.TEXT);
        this.text = text;
    }
    
    public TextContent(Boolean delta, String text, Integer index) {
        super(ContentType.TEXT);
        this.setDelta(delta);
        this.text = text;
        this.setIndex(index);
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}
