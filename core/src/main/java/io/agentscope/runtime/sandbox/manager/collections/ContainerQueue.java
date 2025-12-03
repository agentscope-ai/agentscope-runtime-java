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
package io.agentscope.runtime.sandbox.manager.collections;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;

/**
 * Queue interface for container pool management
 */
public interface ContainerQueue {
    
    /**
     * Add a container to the queue
     * 
     * @param item The container model to enqueue
     */
    void enqueue(ContainerModel item);
    
    /**
     * Remove and return a container from the queue
     * 
     * @return The container model, or null if queue is empty
     */
    ContainerModel dequeue();
    
    /**
     * Return the first container without removing it
     * 
     * @return The container model, or null if queue is empty
     */
    ContainerModel peek();
    
    /**
     * Check if the queue is empty
     * 
     * @return true if empty, false otherwise
     */
    boolean isEmpty();
    
    /**
     * Get the number of containers in the queue
     * 
     * @return The size of the queue
     */
    int size();
    
    /**
     * Clear all containers from the queue
     */
    void clear();
}

