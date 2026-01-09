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

package io.agentscope.runtime.sandbox.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;
import io.agentscope.runtime.sandbox.manager.utils.SandboxMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RedisSandboxMap implements SandboxMap {
    private static final Logger logger = LoggerFactory.getLogger(RedisSandboxMap.class);

    private final RedisClientWrapper redisClient;
    private final ObjectMapper objectMapper;

    private static final String ID_TO_MODEL_PREFIX = "id_to_model:";
    private static final String KEY_TO_ID_PREFIX = "key_to_id:";
    private static final String ID_TO_KEY_PREFIX = "id_to_key:";

    private static final String MAIN_DATA_PREFIX = "sandbox:";

    public RedisSandboxMap(RedisManagerConfig redisManagerConfig) {
        try {
            this.redisClient = new RedisClientWrapper(redisManagerConfig);
            String pong = this.redisClient.ping();
            logger.info("Redis connection test: {}", pong);
            this.objectMapper = new ObjectMapper();
        } catch (Exception e) {
            logger.error("Failed to initialize Redis client: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Redis", e);
        }
    }

    private String getKeyToIdKey(SandboxKey key) {
        return MAIN_DATA_PREFIX + KEY_TO_ID_PREFIX + key.userID() + ":" + key.sessionID() + ":" + key.sandboxType();
    }

    private String getIdToKeyKey(String containerId) {
        return MAIN_DATA_PREFIX + ID_TO_KEY_PREFIX + containerId;
    }

    private String getIdToModelKey(String containerId) {
        return MAIN_DATA_PREFIX + ID_TO_MODEL_PREFIX + containerId;
    }

    @Override
    public void addSandbox(SandboxKey sandboxKey, ContainerModel containerModel) {
        if (sandboxKey == null || containerModel == null) {
            logger.warn("Attempted to put null key or value, skipping");
            return;
        }

        String containerId = containerModel.getContainerId();
        if (containerId == null || containerId.isEmpty()) {
            throw new IllegalArgumentException("ContainerModel must have a non-empty sandboxId");
        }

        String keyToIdKey = getKeyToIdKey(sandboxKey);
        String idToKeyKey = getIdToKeyKey(containerId);
        String idToModelKey = getIdToModelKey(containerId);

        try {
            String containerJson = objectMapper.writeValueAsString(containerModel);
            String sandboxKeyJson = objectMapper.writeValueAsString(sandboxKey);

            redisClient.set(keyToIdKey, containerId);
            redisClient.set(idToKeyKey, sandboxKeyJson);
            redisClient.set(idToModelKey, containerJson);

            logger.info("Added container {} with key {} and id {}",
                    containerModel.getContainerName(), keyToIdKey, containerId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize during addSandbox: {}", e.getMessage());
            throw new RuntimeException("Failed to add sandbox to Redis", e);
        }
    }

    @Override
    public ContainerModel getSandbox(SandboxKey sandboxKey) {
        if (sandboxKey == null) return null;
        String keyToIdKey = getKeyToIdKey(sandboxKey);
        String containerId = redisClient.get(keyToIdKey);
        if (containerId == null || containerId.isEmpty()) {
            return null;
        }
        String idToModelKey = getIdToModelKey(containerId);
        String json = redisClient.get(idToModelKey);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            ContainerModel model = objectMapper.readValue(json, ContainerModel.class);
            logger.debug("Retrieved container {} from Redis", model.getContainerName());
            return model;
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize ContainerModel: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize container from Redis", e);
        }
    }

    @Override
    public ContainerModel getSandbox(String containerId) {
        if (containerId == null || containerId.isEmpty()) return null;
        String idToModelKey = getIdToModelKey(containerId);
        String json = redisClient.get(idToModelKey);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            ContainerModel model = objectMapper.readValue(json, ContainerModel.class);
            logger.debug("Retrieved container {} from Redis", model.getContainerName());
            return model;
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize ContainerModel: {}", e.getMessage());
            throw new RuntimeException("Failed to deserialize container from Redis", e);
        }
    }

    @Override
    public boolean removeSandbox(SandboxKey sandboxKey) {
        if (sandboxKey == null) return false;

        String keyToIdKey = getKeyToIdKey(sandboxKey);
        String containerId = redisClient.get(keyToIdKey);
        if (containerId == null || containerId.isEmpty()) {
            return false;
        }

        redisClient.delete(keyToIdKey);
        redisClient.delete(getIdToKeyKey(containerId));
        redisClient.delete(getIdToModelKey(containerId));

        logger.info("Removed sandbox by key: {}, containerId: {}", sandboxKey, containerId);
        return true;
    }

    @Override
    public void removeSandbox(String containerId) {
        if (containerId == null || containerId.isEmpty()) return;

        String idToKeyKey = getIdToKeyKey(containerId);
        String sandboxKeyJson = redisClient.get(idToKeyKey);
        if (sandboxKeyJson != null && !sandboxKeyJson.isEmpty()) {
            try {
                SandboxKey key = objectMapper.readValue(sandboxKeyJson, SandboxKey.class);
                String keyToIdKey = getKeyToIdKey(key);
                redisClient.delete(keyToIdKey);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to deserialize SandboxKey when removing sandbox by containerId: {}", e.getMessage());
            }
        }

        redisClient.delete(idToKeyKey);
        redisClient.delete(getIdToModelKey(containerId));

        logger.info("Removed sandbox by containerId: {}", containerId);
    }

    @Override
    public boolean containSandbox(SandboxKey sandboxKey) {
        if (sandboxKey == null) return false;
        String keyToIdKey = getKeyToIdKey(sandboxKey);
        String containerId = redisClient.get(keyToIdKey);
        if (containerId == null || containerId.isEmpty()) {
            return false;
        }
        return containSandbox(containerId);
    }

    @Override
    public boolean containSandbox(String containerId) {
        if (containerId == null || containerId.isEmpty()) return false;
        return redisClient.exists(getIdToModelKey(containerId));
    }

    @Override
    public Map<String, ContainerModel> getAllSandboxes() {
        Map<String, ContainerModel> result = new HashMap<>();

        Set<String> keys = redisClient.scan(MAIN_DATA_PREFIX + ID_TO_MODEL_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return result;
        }

        for (String fullKey : keys) {
            String json = redisClient.get(fullKey);
            if (json == null || json.isEmpty()) continue;

            try {
                ContainerModel model = objectMapper.readValue(json, ContainerModel.class);
                result.put(model.getContainerId(), model);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize ContainerModel for key {}: {}", fullKey, e.getMessage());
            }
        }

        logger.debug("Retrieved {} sandboxes from Redis", result.size());
        return result;
    }
}