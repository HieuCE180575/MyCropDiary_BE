package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistRuleRepository extends JpaRepository<ChecklistRule, Integer> {

    @Query("SELECT r FROM ChecklistRule r WHERE r.farm.id = :farmId ORDER BY r.createdAt DESC")
    List<ChecklistRule> findByFarmIdOrderByCreatedAtDesc(@Param("farmId") Integer farmId);

    @Query("SELECT r FROM ChecklistRule r WHERE r.farm.id = :farmId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<ChecklistRule> findActiveByFarmId(@Param("farmId") Integer farmId);
}

