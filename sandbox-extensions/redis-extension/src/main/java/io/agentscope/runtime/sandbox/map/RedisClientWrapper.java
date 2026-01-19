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

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Redis Client wrapper for managing Redis connections and operations
 * Uses Lettuce as the underlying Redis client
 */
public class RedisClientWrapper implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RedisClientWrapper.class);
    
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> syncCommands;
    
    /**
     * Constructor with RedisManagerConfig
     * 
     * @param config Redis manager configuration
     */
    public RedisClientWrapper(RedisManagerConfig config) {
        logger.info("Initializing Redis client with server: {}:{}", config.getRedisServer(), config.getRedisPort());
        
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(config.getRedisServer())
                .withPort(config.getRedisPort())
                .withDatabase(config.getRedisDb());
        
        if (config.getRedisPassword() != null && !config.getRedisPassword().isEmpty()) {
            uriBuilder.withPassword(config.getRedisPassword().toCharArray());
        }
        
        if (config.getRedisUser() != null && !config.getRedisUser().isEmpty()) {
            uriBuilder.withAuthentication(config.getRedisUser(), config.getRedisPassword());
        }
        
        RedisURI redisUri = uriBuilder.build();
        
        this.redisClient = RedisClient.create(redisUri);
        this.connection = redisClient.connect();
        this.syncCommands = connection.sync();
        
        logger.info("Redis client initialized successfully");
    }
    
    /**
     * Set a key-value pair
     * 
     * @param key the key
     * @param value the value
     */
    public void set(String key, String value) {
        syncCommands.set(key, value);
    }
    
    /**
     * Get value by key
     * 
     * @param key the key
     * @return the value, or null if key doesn't exist
     */
    public String get(String key) {
        return syncCommands.get(key);
    }
    
    /**
     * Delete a key
     * 
     * @param key the key to delete
     * @return the number of keys that were removed
     */
    public Long delete(String key) {
        return syncCommands.del(key);
    }
    
    /**
     * Check if a key exists
     * 
     * @param key the key to check
     * @return true if key exists, false otherwise
     */
    public boolean exists(String key) {
        return syncCommands.exists(key) > 0;
    }
    
    /**
     * Get all keys matching a pattern
     * 
     * @param pattern the pattern to match
     * @return list of matching keys
     */
    public List<String> keys(String pattern) {
        return syncCommands.keys(pattern);
    }
    
    /**
     * Scan keys matching a pattern using Redis SCAN command
     * 
     * @param pattern the pattern to match
     * @return set of matching keys
     */
    public Set<String> scan(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanArgs scanArgs = ScanArgs.Builder.limit(100).match(pattern);
        KeyScanCursor<String> cursor = syncCommands.scan(scanArgs);
        
        while (cursor != null) {
            keys.addAll(cursor.getKeys());
            if (cursor.isFinished()) {
                break;
            }
            cursor = syncCommands.scan(cursor, scanArgs);
        }
        return keys;
    }
    
    /**
     * Set expiration on a key
     * 
     * @param key the key
     * @param seconds expiration time in seconds
     * @return true if timeout was set, false otherwise
     */
    public boolean expire(String key, long seconds) {
        return syncCommands.expire(key, seconds);
    }
    
    /**
     * Get the time to live for a key
     * 
     * @param key the key
     * @return TTL in seconds, or -2 if key doesn't exist, -1 if no timeout
     */
    public Long ttl(String key) {
        return syncCommands.ttl(key);
    }

    /**
     * Push value to the right end of a list
     * 
     * @param key the list key
     * @param value the value to push
     * @return the length of the list after the push
     */
    public Long rpush(String key, String value) {
        return syncCommands.rpush(key, value);
    }
    
    /**
     * Pop value from the left end of a list
     * 
     * @param key the list key
     * @return the popped value, or null if list is empty
     */
    public String lpop(String key) {
        return syncCommands.lpop(key);
    }
    
    /**
     * Get element at index in a list
     * 
     * @param key the list key
     * @param index the index
     * @return the element at index, or null
     */
    public String lindex(String key, long index) {
        return syncCommands.lindex(key, index);
    }
    
    /**
     * Get the length of a list
     * 
     * @param key the list key
     * @return the length of the list
     */
    public Long llen(String key) {
        return syncCommands.llen(key);
    }
    
    /**
     * Get range of elements from a list
     * 
     * @param key the list key
     * @param start start index
     * @param stop stop index
     * @return list of elements in the range
     */
    public List<String> lrange(String key, long start, long stop) {
        return syncCommands.lrange(key, start, stop);
    }
    
    /**
     * Remove all elements from a list
     * 
     * @param key the list key
     */
    public void ltrim(String key) {
        syncCommands.ltrim(key, 1, 0); // This removes all elements
    }
    
    // Set operations
    
    /**
     * Add members to a set
     * 
     * @param key the set key
     * @param members the members to add
     * @return the number of members added
     */
    public Long sadd(String key, String... members) {
        return syncCommands.sadd(key, members);
    }
    
    /**
     * Remove members from a set
     * 
     * @param key the set key
     * @param members the members to remove
     * @return the number of members removed
     */
    public Long srem(String key, String... members) {
        return syncCommands.srem(key, members);
    }
    
    /**
     * Check if a member exists in a set
     * 
     * @param key the set key
     * @param member the member to check
     * @return true if member exists, false otherwise
     */
    public boolean sismember(String key, String member) {
        return syncCommands.sismember(key, member);
    }
    
    /**
     * Get all members of a set
     * 
     * @param key the set key
     * @return set of all members
     */
    public Set<String> smembers(String key) {
        return syncCommands.smembers(key);
    }
    
    /**
     * Get the size of a set
     * 
     * @param key the set key
     * @return the size of the set
     */
    public Long scard(String key) {
        return syncCommands.scard(key);
    }
    
    /**
     * Ping the Redis server
     * 
     * @return "PONG" if successful
     */
    public String ping() {
        return syncCommands.ping();
    }
    
    /**
     * Get the underlying sync commands
     * 
     * @return Redis sync commands
     */
    public RedisCommands<String, String> getSyncCommands() {
        return syncCommands;
    }
    
    /**
     * Close the Redis connection
     */
    @Override
    public void close() {
        logger.info("Closing Redis connection");
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        logger.info("Redis connection closed");
    }
}
