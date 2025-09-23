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
 * 会话消息数据访问接口
 */
@Repository
public interface SessionMessageRepository extends JpaRepository<SessionMessageEntity, Long> {
    
    /**
     * 根据会话ID查找消息
     */
    List<SessionMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    /**
     * 根据会话ID分页查找消息
     */
    Page<SessionMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);
    
    /**
     * 根据会话ID删除消息
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 根据会话ID统计消息数量
     */
    long countBySessionId(String sessionId);
    
    /**
     * 获取会话的最新消息
     */
    @Query("SELECT sm FROM SessionMessageEntity sm WHERE sm.sessionId = :sessionId ORDER BY sm.createdAt DESC")
    List<SessionMessageEntity> findLatestBySessionId(@Param("sessionId") String sessionId, Pageable pageable);
}
