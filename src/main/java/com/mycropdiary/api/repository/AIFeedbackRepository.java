package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AIFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIFeedbackRepository extends JpaRepository<AIFeedback, Integer> {

    List<AIFeedback> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<AIFeedback> findByMessageIdAndUserId(Integer messageId, Integer userId);
}

