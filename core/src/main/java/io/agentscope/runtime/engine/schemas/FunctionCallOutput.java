package io.agentscope.runtime.engine.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the output of a function call.
 */
public class FunctionCallOutput {
    @JsonProperty("call_id")
    private String callId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("output")
    private String output;
    
    public FunctionCallOutput() {
    }
    
    public FunctionCallOutput(String callId, String name, String output) {
        this.callId = callId;
        this.name = name;
        this.output = output;
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
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
}

