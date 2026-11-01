package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.InternalAssessmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalAssessmentItemRepository extends JpaRepository<InternalAssessmentItem, Integer> {

    @Query("SELECT item FROM InternalAssessmentItem item " +
            "JOIN FETCH item.assessmentCriterion c " +
            "WHERE item.internalAssessment.id = :assessmentId " +
            "ORDER BY c.id ASC")
    List<InternalAssessmentItem> findByInternalAssessmentIdWithCriterion(@Param("assessmentId") Integer assessmentId);

    void deleteByInternalAssessmentId(Integer internalAssessmentId);
}

