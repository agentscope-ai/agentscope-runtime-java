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

