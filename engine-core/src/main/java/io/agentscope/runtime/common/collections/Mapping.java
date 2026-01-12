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

