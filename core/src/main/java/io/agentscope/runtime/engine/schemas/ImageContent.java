package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Image content in a message.
 */
public class ImageContent extends Content {
    @JsonProperty("image_url")
    private String imageUrl;
    
    public ImageContent() {
        this.setType(ContentType.IMAGE);
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

