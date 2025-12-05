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

