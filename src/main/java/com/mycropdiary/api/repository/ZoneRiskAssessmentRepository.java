package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.ZoneRiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneRiskAssessmentRepository extends JpaRepository<ZoneRiskAssessment, Integer> {

    @Query("SELECT r FROM ZoneRiskAssessment r " +
            "JOIN FETCH r.zone z " +
            "LEFT JOIN FETCH r.assessedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE z.id = :zoneId " +
            "ORDER BY r.assessedAt DESC")
    List<ZoneRiskAssessment> findByZoneIdOrderByAssessedAtDesc(@Param("zoneId") Integer zoneId);

    @Query("SELECT r FROM ZoneRiskAssessment r " +
            "JOIN FETCH r.zone z " +
            "JOIN FETCH z.farm f " +
            "LEFT JOIN FETCH r.assessedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE f.id = :farmId " +
            "ORDER BY r.assessedAt DESC")
    List<ZoneRiskAssessment> findByFarmIdOrderByAssessedAtDesc(@Param("farmId") Integer farmId);
}

