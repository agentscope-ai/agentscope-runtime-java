package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a function call in a message.
 */
public class FunctionCall {
    @JsonProperty("call_id")
    private String callId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("arguments")
    private String arguments;
    
    public FunctionCall() {
    }
    
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
}

