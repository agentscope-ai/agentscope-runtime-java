package io.agentscope.runtime.engine.schemas.agent;

import java.util.Map;

/**
 * Data content class
 * Corresponds to the DataContent class in agent_schemas.py of the Python version
 */
public class DataContent extends Content {
    
    private Map<String, Object> data;
    
    public DataContent() {
        super(ContentType.DATA);
    }
    
    public DataContent(Map<String, Object> data) {
        super(ContentType.DATA);
        this.data = data;
    }
    
    public DataContent(Boolean delta, Map<String, Object> data, Integer index) {
        super(ContentType.DATA);
        this.setDelta(delta);
        this.data = data;
        this.setIndex(index);
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
