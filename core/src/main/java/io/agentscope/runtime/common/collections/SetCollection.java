package io.agentscope.runtime.common.collections;

import java.util.List;

/**
 * Abstract base class for set collection implementations.
 */
public abstract class SetCollection {
    /**
     * Adds a value to the set.
     * 
     * @param value The value to add
     * @return true if added, false if already present
     */
    public abstract boolean add(String value);
    
    /**
     * Removes a value from the set.
     * 
     * @param value The value to remove
     */
    public abstract void remove(String value);
    
    /**
     * Checks if the set contains a value.
     * 
     * @param value The value to check
     * @return true if present, false otherwise
     */
    public abstract boolean contains(String value);
    
    /**
     * Clears all values from the set.
     */
    public abstract void clear();
    
    /**
     * Converts the set to a list.
     * 
     * @return List of all values
     */
    public abstract List<String> toList();
}

