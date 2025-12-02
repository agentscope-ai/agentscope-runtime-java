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

