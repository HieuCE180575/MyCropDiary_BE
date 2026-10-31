package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.SoilAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoilAssessmentRepository extends JpaRepository<SoilAssessment, Integer> {

    @Query("SELECT s FROM SoilAssessment s " +
            "JOIN FETCH s.block b " +
            "JOIN FETCH b.zone z " +
            "LEFT JOIN FETCH s.conductedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE b.id = :blockId " +
            "ORDER BY s.assessedAt DESC")
    List<SoilAssessment> findByBlockIdOrderByAssessedAtDesc(@Param("blockId") Integer blockId);

    @Query("SELECT s FROM SoilAssessment s " +
            "JOIN FETCH s.block b " +
            "JOIN FETCH b.zone z " +
            "JOIN FETCH z.farm f " +
            "LEFT JOIN FETCH s.conductedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE f.id = :farmId " +
            "ORDER BY s.assessedAt DESC")
    List<SoilAssessment> findByFarmIdOrderByAssessedAtDesc(@Param("farmId") Integer farmId);
}
