package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, Integer> {

    List<AIMessage> findByConversationIdOrderByCreatedAtAsc(Integer conversationId);

    Optional<AIMessage> findTopByConversationIdOrderByCreatedAtDesc(Integer conversationId);

    long countByConversationId(Integer conversationId);
}

