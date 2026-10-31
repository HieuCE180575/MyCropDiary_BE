package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.AssessmentCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentCriterionRepository extends JpaRepository<AssessmentCriterion, Integer> {
    List<AssessmentCriterion> findByFarmIdOrderByCriterionNameAsc(Integer farmId);
    List<AssessmentCriterion> findByFarmIdAndIsActiveOrderByCriterionNameAsc(Integer farmId, Boolean isActive);
    Optional<AssessmentCriterion> findByFarmIdAndCriterionName(Integer farmId, String criterionName);
    boolean existsByFarmIdAndCriterionName(Integer farmId, String criterionName);
}

