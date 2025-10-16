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

import io.agentscope.runtime.engine.memory.persistence.memory.entity.SessionMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Session message data access interface
 */
@Repository
public interface SessionMessageRepository extends JpaRepository<SessionMessageEntity, Long> {
    
    /**
     * Find messages by session ID
     */
    List<SessionMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    /**
     * Find messages by session ID with pagination
     */
    Page<SessionMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);
    
    /**
     * Delete messages by session ID
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * Count messages by session ID
     */
    long countBySessionId(String sessionId);
    
    /**
     * Get latest messages for session
     */
    @Query("SELECT sm FROM SessionMessageEntity sm WHERE sm.sessionId = :sessionId ORDER BY sm.createdAt DESC")
    List<SessionMessageEntity> findLatestBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
}
