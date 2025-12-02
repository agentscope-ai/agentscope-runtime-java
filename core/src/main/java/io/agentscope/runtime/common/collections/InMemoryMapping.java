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

