package io.agentscope.runtime.common.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * In-memory implementation of the Queue interface.
 */
public class InMemoryQueue extends Queue {
    private final Deque<Map<String, Object>> queue;
    
    public InMemoryQueue() {
        this.queue = new ArrayDeque<>();
    }
    
    @Override
    public void enqueue(Map<String, Object> item) {
        queue.addLast(item);
    }
    
    @Override
    public Map<String, Object> dequeue() {
        if (queue.isEmpty()) {
            return null;
        }
        return queue.removeFirst();
    }
    
    @Override
    public Map<String, Object> peek() {
        if (queue.isEmpty()) {
            return null;
        }
        return queue.peekFirst();
    }
    
    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    @Override
    public int size() {
        return queue.size();
    }
}

