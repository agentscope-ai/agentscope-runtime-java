package com.example.agentscope.model;

import java.util.Map;

/**
 * Tool info model
 */
public class ToolInfo {
    
    private String name;
    private String description;
    private Map<String, Object> parameters;

    public ToolInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "ToolInfo{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}

