package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.WaterAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaterAssessmentRepository extends JpaRepository<WaterAssessment, Integer> {

    List<WaterAssessment> findByBlockIdOrderByAssessedAtDesc(Integer blockId);

    @Query("SELECT wa FROM WaterAssessment wa WHERE wa.waterSource.farm.id = :farmId ORDER BY wa.assessedAt DESC")
    List<WaterAssessment> findByFarmIdOrderByAssessedAtDesc(@Param("farmId") Integer farmId);
}

