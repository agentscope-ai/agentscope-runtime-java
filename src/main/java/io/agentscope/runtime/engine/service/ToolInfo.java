package io.agentscope.runtime.engine.service;

import java.util.Map;

/**
 * Tool information class
 * Corresponds to the tool information structure in the Python version
 */
public class ToolInfo {
    
    private String name;
    private String description;
    private Map<String, Object> schema;
    private String source;
    private String group;
    
    public ToolInfo() {}
    
    public ToolInfo(String name, String description, Map<String, Object> schema) {
        this.name = name;
        this.description = description;
        this.schema = schema;
    }
    
    public ToolInfo(String name, String description, Map<String, Object> schema, String source, String group) {
        this.name = name;
        this.description = description;
        this.schema = schema;
        this.source = source;
        this.group = group;
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
    
    public Map<String, Object> getSchema() {
        return schema;
    }
    
    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
}
