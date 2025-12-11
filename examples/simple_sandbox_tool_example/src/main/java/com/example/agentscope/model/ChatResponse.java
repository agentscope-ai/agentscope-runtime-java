package com.example.agentscope.model;

/**
 * Chat response model
 */
public class ChatResponse {
    
    private boolean success;
    private String message;
    private String agentName;
    private Long timestamp;
    private Long processingTime;
    private String error;

    public ChatResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", agentName='" + agentName + '\'' +
                ", timestamp=" + timestamp +
                ", processingTime=" + processingTime +
                ", error='" + error + '\'' +
                '}';
    }
}

