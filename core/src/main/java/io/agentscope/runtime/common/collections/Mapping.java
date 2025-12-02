package io.agentscope.runtime.common.collections;

import java.util.List;

/**
 * Abstract base class for key-value mapping implementations.
 */
public abstract class Mapping {
    /**
     * Sets a key-value pair.
     * 
     * @param key The key
     * @param value The value (as a Map)
     */
    public abstract void set(String key, Object value);
    
    /**
     * Gets the value for a key.
     * 
     * @param key The key
     * @return The value, or null if not found
     */
    public abstract Object get(String key);
    
    /**
     * Deletes a key-value pair.
     * 
     * @param key The key to delete
     */
    public abstract void delete(String key);
    
    /**
     * Scans for keys with the given prefix.
     * 
     * @param prefix The prefix to match
     * @return List of matching keys
     */
    public abstract List<String> scan(String prefix);
}

