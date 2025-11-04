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

import io.agentscope.runtime.engine.memory.persistence.memory.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Session data access interface
 */
@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    
    /**
     * Find session by session ID
     */
    Optional<SessionEntity> findBySessionId(String sessionId);
    
    /**
     * Find sessions by user ID
     */
    List<SessionEntity> findByUserId(String userId);

    /**
     * Find session by user ID and session ID
     */
    Optional<SessionEntity> findByUserIdAndSessionId(String userId, String sessionId);
    
    /**
     * Delete sessions by user ID
     */
    void deleteByUserId(String userId);
    
    /**
     * Delete session by session ID
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * Get all sessions for user
     */
    List<SessionEntity> findByUserIdOrderByLastActivityDesc(String userId);
    
    /**
     * Get all sessions
     */
    @Query("SELECT s FROM SessionEntity s ORDER BY s.lastActivity DESC")
    List<SessionEntity> findAllOrderByLastActivityDesc();
    
    /**
     * Count sessions by user ID
     */
    long countByUserId(String userId);
    
    /**
     * Check if session exists
     */
    boolean existsBySessionId(String sessionId);
    
    /**
     * Check if user and session exist
     */
    boolean existsByUserIdAndSessionId(String userId, String sessionId);
}
