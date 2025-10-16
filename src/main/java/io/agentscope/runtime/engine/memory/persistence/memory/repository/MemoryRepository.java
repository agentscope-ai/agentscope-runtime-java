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
package io.agentscope.runtime.engine.memory.persistence.memory.repository;

import io.agentscope.runtime.engine.memory.persistence.memory.entity.MemoryEntity;
import io.agentscope.runtime.engine.memory.model.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Memory data access interface
 */
@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {
    
    /**
     * Find memories by user ID
     */
    Page<MemoryEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * Find memories by user ID and session ID
     */
    Page<MemoryEntity> findByUserIdAndSessionIdOrderByCreatedAtDesc(String userId, String sessionId, Pageable pageable);
    
    /**
     * Delete memories by user ID
     */
    void deleteByUserId(String userId);
    
    /**
     * Delete memories by user ID and session ID
     */
    void deleteByUserIdAndSessionId(String userId, String sessionId);
    
    /**
     * Get all user IDs
     */
    @Query("SELECT DISTINCT m.userId FROM MemoryEntity m")
    List<String> findAllUserIds();
    
    /**
     * Count memories by user ID
     */
    long countByUserId(String userId);
    
    /**
     * Count memories by user ID and message type
     */
    long countByUserIdAndMessageType(String userId, MessageType messageType);
    
    /**
     * Search memories (based on content)
     */
    @Query("SELECT m FROM MemoryEntity m WHERE m.userId = :userId AND m.content LIKE %:keyword% ORDER BY m.createdAt DESC")
    Page<MemoryEntity> searchByUserIdAndContent(@Param("userId") String userId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Get all memories for user (without pagination)
     */
    List<MemoryEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Get all memories (without pagination)
     */
    @Query("SELECT m FROM MemoryEntity m ORDER BY m.createdAt DESC")
    List<MemoryEntity> findAllOrderByCreatedAtDesc();
}
