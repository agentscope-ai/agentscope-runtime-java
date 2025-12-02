package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Data content in a message (for function calls, etc.).
 */
public class DataContent extends Content {
    @JsonProperty("data")
    private Map<String, Object> data;
    
    public DataContent() {
        this.setType(ContentType.DATA);
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}

