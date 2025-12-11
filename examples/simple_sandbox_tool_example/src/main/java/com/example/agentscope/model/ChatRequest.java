package com.example.agentscope.model;

/**
 * Chat request model
 */
public class ChatRequest {
    
    private String message;
    private String userName;

    public ChatRequest() {
    }

    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "message='" + message + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}

