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

package io.agentscope.runtime.engine.schemas;

/**
 * A simple sequence number generator for streaming events.
 * 
 * This class encapsulates the logic for generating sequential numbers,
 * making the code more maintainable and less error-prone.
 */
public class SequenceNumberGenerator {
    private int current;
    
    /**
     * Initialize the generator with a starting number.
     */
    public SequenceNumberGenerator() {
        this(0);
    }
    
    /**
     * Initialize the generator with a starting number.
     * 
     * @param start The starting sequence number
     */
    public SequenceNumberGenerator(int start) {
        this.current = start;
    }
    
    /**
     * Get the next sequence number and increment the counter.
     * 
     * @return The current sequence number before incrementing
     */
    public int next() {
        int result = current;
        current++;
        return result;
    }
    
    /**
     * Set the sequence number on an event and increment the counter.
     * 
     * @param event The event to set the sequence number on
     * @return The same event with sequence number set
     */
    public Event yieldWithSequence(Event event) {
        event.setSequenceNumber(this.next());
        return event;
    }
}

