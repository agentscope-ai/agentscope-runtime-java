package io.agentscope.runtime.engine.schemas.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Function call class
 * Corresponds to the FunctionCall class in agent_schemas.py of the Python version
 */
public class FunctionCall {
    
    private String callId;
    private String name;
    private String arguments;
    
    public FunctionCall() {}
    
    public FunctionCall(String callId, String name, String arguments) {
        this.callId = callId;
        this.name = name;
        this.arguments = arguments;
    }
    
    public String getCallId() {
        return callId;
    }
    
    public void setCallId(String callId) {
        this.callId = callId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getArguments() {
        return arguments;
    }
    
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
    
    /**
     * Convert to Map format
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("arguments", arguments);
        return map;
    }
    
    /**
     * Create FunctionCall from Map
     */
    public static FunctionCall fromMap(Map<String, Object> map) {
        FunctionCall functionCall = new FunctionCall();
        functionCall.setCallId((String) map.get("call_id"));
        functionCall.setName((String) map.get("name"));
        functionCall.setArguments((String) map.get("arguments"));
        return functionCall;
    }
}
