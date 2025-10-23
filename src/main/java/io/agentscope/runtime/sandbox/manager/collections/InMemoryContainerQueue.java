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

import java.util.LinkedList;
import java.util.Queue;

/**
 * In-memory implementation of ContainerQueue
 */
public class InMemoryContainerQueue implements ContainerQueue {
    
    private final Queue<ContainerModel> queue;
    
    public InMemoryContainerQueue() {
        this.queue = new LinkedList<>();
    }
    
    @Override
    public synchronized void enqueue(ContainerModel item) {
        if (item != null) {
            queue.offer(item);
        }
    }
    
    @Override
    public synchronized ContainerModel dequeue() {
        return queue.poll();
    }
    
    @Override
    public synchronized ContainerModel peek() {
        return queue.peek();
    }
    
    @Override
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
    
    @Override
    public synchronized int size() {
        return queue.size();
    }
    
    @Override
    public synchronized void clear() {
        queue.clear();
    }
}

