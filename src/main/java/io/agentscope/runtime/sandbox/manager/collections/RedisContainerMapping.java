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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.util.RedisClientWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Redis-backed implementation for container mapping
 * Maps SandboxKey to ContainerModel using Redis
 */
public class RedisContainerMapping {
    
    private static final Logger logger = Logger.getLogger(RedisContainerMapping.class.getName());
    
    private final RedisClientWrapper redisClient;
    private final String prefix;
    private final ObjectMapper objectMapper;

    public RedisContainerMapping(RedisClientWrapper redisClient, String prefix) {
        this.redisClient = redisClient;
        this.prefix = (prefix != null && !prefix.isEmpty()) ?
                      (prefix.endsWith(":") ? prefix : prefix + ":") : "";
        this.objectMapper = new ObjectMapper();
        logger.info("Redis container mapping initialized with prefix: " + this.prefix);
    }

    private String getFullKey(SandboxKey key) {
        return String.format("%s%s:%s:%s",
                prefix, 
                key.getUserID(), 
                key.getSessionID(), 
                key.getSandboxType().getTypeName());
    }

    private String getFullKey(String key) {
        return prefix + key;
    }

    private String stripPrefix(String fullKey) {
        if (prefix != null && !prefix.isEmpty() && fullKey.startsWith(prefix)) {
            return fullKey.substring(prefix.length());
        }
        return fullKey;
    }

    public void put(SandboxKey key, ContainerModel value) {
        if (key == null || value == null) {
            logger.warning("Attempted to put null key or value, skipping");
            return;
        }
        
        try {
            String fullKey = getFullKey(key);
            String json = objectMapper.writeValueAsString(value);
            redisClient.set(fullKey, json);
            logger.fine("Put container in Redis: " + value.getContainerName() + " with key: " + fullKey);
        } catch (JsonProcessingException e) {
            logger.severe("Failed to serialize container model: " + e.getMessage());
            throw new RuntimeException("Failed to put container in Redis", e);
        }
    }

    public ContainerModel get(SandboxKey key) {
        if (key == null) {
            return null;
        }
        
        try {
            String fullKey = getFullKey(key);
            String json = redisClient.get(fullKey);
            
            if (json == null || json.isEmpty()) {
                return null;
            }
            
            ContainerModel model = objectMapper.readValue(json, ContainerModel.class);
            logger.fine("Retrieved container from Redis: " + model.getContainerName());
            return model;
        } catch (JsonProcessingException e) {
            logger.severe("Failed to deserialize container model: " + e.getMessage());
            throw new RuntimeException("Failed to get container from Redis", e);
        }
    }

    public boolean remove(SandboxKey key) {
        if (key == null) {
            return false;
        }
        
        String fullKey = getFullKey(key);
        Long deleted = redisClient.delete(fullKey);
        boolean removed = deleted != null && deleted > 0;
        
        if (removed) {
            logger.fine("Removed container from Redis with key: " + fullKey);
        }
        
        return removed;
    }

    public boolean containsKey(SandboxKey key) {
        if (key == null) {
            return false;
        }
        
        String fullKey = getFullKey(key);
        return redisClient.exists(fullKey);
    }

    public Set<String> scan(String pattern) {
        String searchPattern = getFullKey(pattern) + "*";
        Set<String> fullKeys = redisClient.scan(searchPattern);
        
        Set<String> keys = new java.util.HashSet<>();
        for (String fullKey : fullKeys) {
            keys.add(stripPrefix(fullKey));
        }
        
        return keys;
    }

    public Map<SandboxKey, ContainerModel> getAll() {
        Map<SandboxKey, ContainerModel> result = new HashMap<>();
        
        String searchPattern = prefix + "*";
        Set<String> fullKeys = redisClient.scan(searchPattern);
        
        for (String fullKey : fullKeys) {
            try {
                String json = redisClient.get(fullKey);
                if (json != null && !json.isEmpty()) {
                    ContainerModel model = objectMapper.readValue(json, ContainerModel.class);

                    String keyWithoutPrefix = stripPrefix(fullKey);
                    SandboxKey sandboxKey = parseSandboxKey(keyWithoutPrefix);
                    
                    if (sandboxKey != null) {
                        result.put(sandboxKey, model);
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to deserialize container for key " + fullKey + ": " + e.getMessage());
            }
        }
        
        return result;
    }

    private SandboxKey parseSandboxKey(String keyString) {
        if (keyString == null || keyString.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = keyString.split(":", 3);
            if (parts.length != 3) {
                logger.warning("Invalid key format: " + keyString);
                return null;
            }
            
            String userID = parts[0];
            String sessionID = parts[1];
            String sandboxTypeStr = parts[2];
            
            SandboxType sandboxType = SandboxType.valueOf(sandboxTypeStr.toUpperCase());
            
            return new SandboxKey(userID, sessionID, sandboxType);
        } catch (Exception e) {
            logger.warning("Failed to parse SandboxKey from: " + keyString + ", error: " + e.getMessage());
            return null;
        }
    }

    public void clear() {
        String searchPattern = prefix + "*";
        Set<String> fullKeys = redisClient.scan(searchPattern);
        
        for (String fullKey : fullKeys) {
            redisClient.delete(fullKey);
        }
        
        logger.info("Cleared all entries with prefix: " + prefix);
    }

    public int size() {
        String searchPattern = prefix + "*";
        Set<String> fullKeys = redisClient.scan(searchPattern);
        return fullKeys.size();
    }
}

