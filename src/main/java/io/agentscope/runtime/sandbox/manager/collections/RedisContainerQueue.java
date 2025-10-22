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
import io.agentscope.runtime.sandbox.manager.util.RedisClientWrapper;

import java.util.logging.Logger;

/**
 * Redis-backed implementation of ContainerQueue
 */
public class RedisContainerQueue implements ContainerQueue {
    
    private static final Logger logger = Logger.getLogger(RedisContainerQueue.class.getName());
    
    private final RedisClientWrapper redisClient;
    private final String queueName;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor
     * 
     * @param redisClient Redis client wrapper
     * @param queueName the name of the Redis queue
     */
    public RedisContainerQueue(RedisClientWrapper redisClient, String queueName) {
        this.redisClient = redisClient;
        this.queueName = queueName;
        this.objectMapper = new ObjectMapper();
        logger.info("Redis container queue initialized with name: " + queueName);
    }
    
    @Override
    public void enqueue(ContainerModel item) {
        if (item == null) {
            logger.warning("Attempted to enqueue null item, skipping");
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(item);
            redisClient.rpush(queueName, json);
            logger.fine("Enqueued container: " + item.getContainerName());
        } catch (JsonProcessingException e) {
            logger.severe("Failed to serialize container model: " + e.getMessage());
            throw new RuntimeException("Failed to enqueue container", e);
        }
    }
    
    @Override
    public ContainerModel dequeue() {
        try {
            String json = redisClient.lpop(queueName);
            if (json == null || json.isEmpty()) {
                return null;
            }
            
            ContainerModel model = objectMapper.readValue(json, ContainerModel.class);
            logger.fine("Dequeued container: " + model.getContainerName());
            return model;
        } catch (JsonProcessingException e) {
            logger.severe("Failed to deserialize container model: " + e.getMessage());
            throw new RuntimeException("Failed to dequeue container", e);
        }
    }
    
    @Override
    public ContainerModel peek() {
        try {
            String json = redisClient.lindex(queueName, 0);
            if (json == null || json.isEmpty()) {
                return null;
            }
            
            return objectMapper.readValue(json, ContainerModel.class);
        } catch (JsonProcessingException e) {
            logger.severe("Failed to deserialize container model: " + e.getMessage());
            throw new RuntimeException("Failed to peek container", e);
        }
    }
    
    @Override
    public boolean isEmpty() {
        Long length = redisClient.llen(queueName);
        return length == null || length == 0;
    }
    
    @Override
    public int size() {
        Long length = redisClient.llen(queueName);
        return length != null ? length.intValue() : 0;
    }
    
    @Override
    public void clear() {
        redisClient.ltrim(queueName);
        logger.info("Cleared Redis queue: " + queueName);
    }
}

