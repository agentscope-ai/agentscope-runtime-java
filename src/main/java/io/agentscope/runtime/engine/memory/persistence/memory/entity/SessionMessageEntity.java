package io.agentscope.runtime.engine.memory.persistence.memory.entity;

import io.agentscope.runtime.engine.memory.model.MessageType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Session message entity class
 */
@Entity
@Table(name = "session_messages", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class SessionMessageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "session_id", insertable = false, updatable = false)
    private SessionEntity session;
    
    public SessionMessageEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SessionMessageEntity(String sessionId, MessageType messageType, String content) {
        this();
        this.sessionId = sessionId;
        this.messageType = messageType;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public SessionEntity getSession() {
        return session;
    }
    
    public void setSession(SessionEntity session) {
        this.session = session;
    }
}
