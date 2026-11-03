package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Integer> {

    List<AIConversation> findByUserIdOrderByUpdatedAtDesc(Integer userId);

    @Query("SELECT c FROM AIConversation c WHERE c.user.id = :userId AND " +
           "(LOWER(c.conversationTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT m FROM AIMessage m WHERE m.conversation = c AND LOWER(m.messageText) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "ORDER BY c.updatedAt DESC")
    List<AIConversation> searchByUserIdAndKeyword(@Param("userId") Integer userId, @Param("keyword") String keyword);

    Optional<AIConversation> findByIdAndUserId(Integer id, Integer userId);
}

