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
 * 记忆数据访问接口
 */
@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {
    
    /**
     * 根据用户ID查找记忆
     */
    Page<MemoryEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 根据用户ID和会话ID查找记忆
     */
    Page<MemoryEntity> findByUserIdAndSessionIdOrderByCreatedAtDesc(String userId, String sessionId, Pageable pageable);
    
    /**
     * 根据用户ID删除记忆
     */
    void deleteByUserId(String userId);
    
    /**
     * 根据用户ID和会话ID删除记忆
     */
    void deleteByUserIdAndSessionId(String userId, String sessionId);
    
    /**
     * 获取所有用户ID
     */
    @Query("SELECT DISTINCT m.userId FROM MemoryEntity m")
    List<String> findAllUserIds();
    
    /**
     * 根据用户ID统计记忆数量
     */
    long countByUserId(String userId);
    
    /**
     * 根据用户ID和消息类型统计记忆数量
     */
    long countByUserIdAndMessageType(String userId, MessageType messageType);
    
    /**
     * 搜索记忆（基于内容）
     */
    @Query("SELECT m FROM MemoryEntity m WHERE m.userId = :userId AND m.content LIKE %:keyword% ORDER BY m.createdAt DESC")
    Page<MemoryEntity> searchByUserIdAndContent(@Param("userId") String userId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 获取用户的所有记忆（不分页）
     */
    List<MemoryEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 获取所有记忆（不分页）
     */
    @Query("SELECT m FROM MemoryEntity m ORDER BY m.createdAt DESC")
    List<MemoryEntity> findAllOrderByCreatedAtDesc();
}
