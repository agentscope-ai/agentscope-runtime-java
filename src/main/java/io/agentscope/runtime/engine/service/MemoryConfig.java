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
package io.agentscope.runtime.engine.service;

import io.agentscope.runtime.engine.memory.service.EmbeddingService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.service.MySQLMemoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.service.RedisMemoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.service.SimpleEmbeddingService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.session.MySQLSessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.session.RedisSessionHistoryService;
import io.agentscope.runtime.engine.memory.persistence.memory.repository.MemoryRepository;
import io.agentscope.runtime.engine.memory.persistence.memory.repository.SessionMessageRepository;
import io.agentscope.runtime.engine.memory.persistence.memory.repository.SessionRepository;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Memory service configuration class
 * Provides Spring Boot auto-configuration support
 */
@Configuration
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryConfig {

    /**
     * Default memory service (memory implementation)
     */
    @Bean
    @ConditionalOnProperty(name = "memory.service.type", havingValue = "memory", matchIfMissing = true)
    public MemoryService inMemoryMemoryService(MemoryProperties memoryProperties) {
        InMemoryMemoryService service = new InMemoryMemoryService();
        service.setMemoryProperties(memoryProperties);
        return service;
    }

    /**
     * Default session history service (memory implementation)
     */
    @Bean
    @ConditionalOnProperty(name = "memory.service.type", havingValue = "memory", matchIfMissing = true)
    public SessionHistoryService inMemorySessionHistoryService() {
        return new InMemorySessionHistoryService();
    }

    /**
     * Redis memory service
     */
    @Bean
    @ConditionalOnProperty(name = "memory.service.type", havingValue = "redis")
    public MemoryService redisMemoryService(RedisTemplate<String, String> redisTemplate, MemoryProperties memoryProperties) {
        RedisMemoryService service = new RedisMemoryService(redisTemplate);
        service.setMemoryProperties(memoryProperties);
        return service;
    }

    /**
     * Redis session history service
     */
    @Bean
    @ConditionalOnProperty(name = "memory.service.type", havingValue = "redis")
    @ConditionalOnMissingBean
    public SessionHistoryService redisSessionHistoryService(RedisTemplate<String, String> redisTemplate) {
        return new RedisSessionHistoryService(redisTemplate);
    }

    /**
     * Default embedding service
     */
    @Bean
    public EmbeddingService embeddingService() {
        return new SimpleEmbeddingService();
    }

    /**
     * MySQL memory service
     */
    @Bean
    @ConditionalOnProperty(name = "memory.service.type", havingValue = "mysql")
    public MemoryService mysqlMemoryService(MemoryRepository memoryRepository, ObjectMapper objectMapper, 
                                          EmbeddingService embeddingService, MemoryProperties memoryProperties) {
        MySQLMemoryService service = new MySQLMemoryService();
        service.setMemoryRepository(memoryRepository);
        service.setObjectMapper(objectMapper);
        service.setEmbeddingService(embeddingService);
        service.setMemoryProperties(memoryProperties);
        return service;
    }

    /**
     * MySQL session history service
     */
    @Bean
    @ConditionalOnProperty(name = "memory.service.type", havingValue = "mysql")
    public SessionHistoryService mysqlSessionHistoryService(SessionRepository sessionRepository, 
                                                           SessionMessageRepository sessionMessageRepository, 
                                                           ObjectMapper objectMapper) {
        MySQLSessionHistoryService service = new MySQLSessionHistoryService();
        service.setSessionRepository(sessionRepository);
        service.setSessionMessageRepository(sessionMessageRepository);
        service.setObjectMapper(objectMapper);
        return service;
    }

    /**
     * Context manager
     */
    @Bean
    public ContextManager contextManager(
            MemoryService memoryService,
            SessionHistoryService sessionHistoryService) {
        return ContextManagerFactory.createCustom(memoryService, sessionHistoryService);
    }
}
