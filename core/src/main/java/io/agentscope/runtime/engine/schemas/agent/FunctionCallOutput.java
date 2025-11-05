package io.agentscope.runtime.engine.schemas.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Function call output class
 * Corresponds to the FunctionCallOutput class in agent_schemas.py of the Python version
 */
public class FunctionCallOutput {
    
    private String callId;
    private String output;
    
    public FunctionCallOutput() {}
    
    public FunctionCallOutput(String callId, String output) {
        this.callId = callId;
        this.output = output;
    }
    
    public String getCallId() {
        return callId;
    }
    
    public void setCallId(String callId) {
        this.callId = callId;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    /**
     * Convert to Map format
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("call_id", callId);
        map.put("output", output);
        return map;
    }
    
    /**
     * Create FunctionCallOutput from Map
     */
    public static FunctionCallOutput fromMap(Map<String, Object> map) {
        FunctionCallOutput output = new FunctionCallOutput();
        output.setCallId((String) map.get("call_id"));
        output.setOutput((String) map.get("output"));
        return output;
    }
}
