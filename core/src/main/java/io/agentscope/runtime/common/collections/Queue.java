/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

