package io.agentscope.chatbot.repository;

import io.agentscope.chatbot.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}

