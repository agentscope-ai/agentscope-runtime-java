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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory implementation of the SetCollection interface.
 */
public class InMemorySetCollection extends SetCollection {
    private final Set<String> set;
    
    public InMemorySetCollection() {
        this.set = new HashSet<>();
    }
    
    @Override
    public boolean add(String value) {
        if (set.contains(value)) {
            return false;
        }
        set.add(value);
        return true;
    }
    
    @Override
    public void remove(String value) {
        set.remove(value);
    }
    
    @Override
    public boolean contains(String value) {
        return set.contains(value);
    }
    
    @Override
    public void clear() {
        set.clear();
    }
    
    @Override
    public List<String> toList() {
        return new ArrayList<>(set);
    }
}

