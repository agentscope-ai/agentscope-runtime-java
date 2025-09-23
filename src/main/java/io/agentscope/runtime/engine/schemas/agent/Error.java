package io.agentscope.runtime.engine.schemas.agent;

/**
 * Error information class
 * Corresponds to the Error class in agent_schemas.py of the Python version
 */
public class Error {
    
    private String code;
    private String message;
    
    public Error() {}
    
    public Error(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
