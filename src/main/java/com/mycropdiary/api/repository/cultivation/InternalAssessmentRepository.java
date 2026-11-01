package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.InternalAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternalAssessmentRepository extends JpaRepository<InternalAssessment, Integer> {

    @Query("SELECT ia FROM InternalAssessment ia " +
            "JOIN FETCH ia.farm f " +
            "LEFT JOIN FETCH ia.cropSeason cs " +
            "LEFT JOIN FETCH ia.zone z " +
            "LEFT JOIN FETCH ia.assessedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE f.id = :farmId " +
            "AND (:cropSeasonId IS NULL OR cs.id = :cropSeasonId) " +
            "AND (:zoneId IS NULL OR z.id = :zoneId) " +
            "ORDER BY ia.assessedAt DESC")
    List<InternalAssessment> findAssessmentsByFarm(
            @Param("farmId") Integer farmId,
            @Param("cropSeasonId") Integer cropSeasonId,
            @Param("zoneId") Integer zoneId
    );

    @Query("SELECT ia FROM InternalAssessment ia " +
            "JOIN FETCH ia.farm f " +
            "LEFT JOIN FETCH ia.cropSeason cs " +
            "LEFT JOIN FETCH ia.zone z " +
            "LEFT JOIN FETCH ia.assessedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE ia.id = :id")
    Optional<InternalAssessment> findByIdWithDetails(@Param("id") Integer id);
}

