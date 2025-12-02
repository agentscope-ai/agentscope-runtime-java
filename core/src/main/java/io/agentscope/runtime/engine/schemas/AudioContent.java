package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audio content in a message.
 */
public class AudioContent extends Content {
    @JsonProperty("data")
    private String data;
    
    @JsonProperty("format")
    private String format;
    
    public AudioContent() {
        this.setType(ContentType.AUDIO);
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
}

