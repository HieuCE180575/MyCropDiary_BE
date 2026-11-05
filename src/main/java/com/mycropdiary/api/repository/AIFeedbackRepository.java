package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AIFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIFeedbackRepository extends JpaRepository<AIFeedback, Integer> {

    List<AIFeedback> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<AIFeedback> findByMessageIdAndUserId(Integer messageId, Integer userId);

    List<AIFeedback> findByMessageIdIn(java.util.Collection<Integer> messageIds);

    List<AIFeedback> findAllByOrderByCreatedAtDesc();

    long countByRepliesIsEmpty();

    long countByRepliesIsNotEmpty();

    @Query("SELECT AVG(CAST(f.rating AS double)) FROM AIFeedback f WHERE f.rating IS NOT NULL")
    Double getAverageRating();
}
