package io.agentscope.runtime.common.collections;

import java.util.Map;

/**
 * Abstract base class for queue implementations.
 */
public abstract class Queue {
    /**
     * Adds an item to the queue.
     * 
     * @param item Item to enqueue (as a Map)
     */
    public abstract void enqueue(Map<String, Object> item);
    
    /**
     * Removes and returns an item from the queue.
     * 
     * @return The dequeued item, or null if queue is empty
     */
    public abstract Map<String, Object> dequeue();
    
    /**
     * Returns the front item without removing it.
     * 
     * @return The front item, or null if queue is empty
     */
    public abstract Map<String, Object> peek();
    
    /**
     * Checks if the queue is empty.
     * 
     * @return true if empty, false otherwise
     */
    public abstract boolean isEmpty();
    
    /**
     * Returns the number of items in the queue.
     * 
     * @return Queue size
     */
    public abstract int size();
}

