package io.agentscope.runtime.engine.agents;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent configuration class
 * Corresponds to agent_config in the Python version
 */
public class AgentConfig {
    
    private Map<String, Object> config;
    
    public AgentConfig() {
        this.config = new HashMap<>();
    }
    
    public AgentConfig(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
    }
    
    public Object get(String key) {
        return config.get(key);
    }
    
    public void set(String key, Object value) {
        config.put(key, value);
    }
    
    public boolean containsKey(String key) {
        return config.containsKey(key);
    }
    
    public Map<String, Object> getAll() {
        return new HashMap<>(config);
    }
    
    public void putAll(Map<String, Object> other) {
        config.putAll(other);
    }
    
    public void clear() {
        config.clear();
    }
    
    public boolean isEmpty() {
        return config.isEmpty();
    }
    
    public int size() {
        return config.size();
    }
    
    @Override
    public String toString() {
        return "AgentConfig{" + config + "}";
    }
}
