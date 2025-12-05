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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of the Mapping interface.
 */
public class InMemoryMapping extends Mapping {
    private final Map<String, Object> store;
    
    public InMemoryMapping() {
        this.store = new HashMap<>();
    }
    
    @Override
    public void set(String key, Object value) {
        store.put(key, value);
    }
    
    @Override
    public Object get(String key) {
        return store.get(key);
    }
    
    @Override
    public void delete(String key) {
        store.remove(key);
    }
    
    @Override
    public List<String> scan(String prefix) {
        List<String> result = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) {
            result.addAll(store.keySet());
        } else {
            for (String key : store.keySet()) {
                if (key.startsWith(prefix)) {
                    result.add(key);
                }
            }
        }
        return result;
    }
}

