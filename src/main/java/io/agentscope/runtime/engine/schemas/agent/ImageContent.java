package io.agentscope.runtime.engine.schemas.agent;

/**
 * Image content class
 * Corresponds to the ImageContent class in agent_schemas.py of the Python version
 */
public class ImageContent extends Content {
    
    private String imageUrl;
    
    public ImageContent() {
        super(ContentType.IMAGE);
    }
    
    public ImageContent(String imageUrl) {
        super(ContentType.IMAGE);
        this.imageUrl = imageUrl;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
