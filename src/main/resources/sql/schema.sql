-- Memory service database initialization script

-- Create database (if not exists)
CREATE DATABASE IF NOT EXISTS memory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE memory_db;

-- Create memory table
CREATE TABLE IF NOT EXISTS memories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255),
    message_type ENUM('MESSAGE', 'SYSTEM', 'USER', 'ASSISTANT') NOT NULL,
    content TEXT,
    metadata JSON,
    embedding JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_user_session (user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create session table
CREATE TABLE IF NOT EXISTS sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_last_activity (last_activity),
    INDEX idx_user_session (user_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create session message table
CREATE TABLE IF NOT EXISTS session_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type ENUM('MESSAGE', 'SYSTEM', 'USER', 'ASSISTANT') NOT NULL,
    content TEXT,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_session_type (session_id, message_type),
    FOREIGN KEY (session_id) REFERENCES sessions(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create user statistics view
CREATE OR REPLACE VIEW user_memory_stats AS
SELECT 
    user_id,
    COUNT(*) as total_memories,
    COUNT(CASE WHEN message_type = 'USER' THEN 1 END) as user_messages,
    COUNT(CASE WHEN message_type = 'ASSISTANT' THEN 1 END) as assistant_messages,
    COUNT(DISTINCT session_id) as total_sessions,
    MAX(created_at) as last_activity
FROM memories 
GROUP BY user_id;

-- Create session statistics view
CREATE OR REPLACE VIEW session_stats AS
SELECT 
    s.user_id,
    s.session_id,
    s.created_at as session_created,
    s.last_activity,
    COUNT(sm.id) as message_count,
    COUNT(CASE WHEN sm.message_type = 'USER' THEN 1 END) as user_messages,
    COUNT(CASE WHEN sm.message_type = 'ASSISTANT' THEN 1 END) as assistant_messages
FROM sessions s
LEFT JOIN session_messages sm ON s.session_id = sm.session_id
GROUP BY s.user_id, s.session_id, s.created_at, s.last_activity;
