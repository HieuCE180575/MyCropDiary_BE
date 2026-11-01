package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.ChecklistRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, Integer> {

    @Query("SELECT r FROM ChecklistRun r " +
            "JOIN FETCH r.checklistRule rule " +
            "JOIN FETCH rule.farm f " +
            "LEFT JOIN FETCH r.cropSeason cs " +
            "LEFT JOIN FETCH r.block b " +
            "LEFT JOIN FETCH r.assignedTo m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE f.id = :farmId " +
            "AND (:cropSeasonId IS NULL OR cs.id = :cropSeasonId) " +
            "AND (:runStatus IS NULL OR LOWER(r.runStatus) = LOWER(:runStatus)) " +
            "ORDER BY r.createdAt DESC")
    List<ChecklistRun> findRunsByFarm(
            @Param("farmId") Integer farmId,
            @Param("cropSeasonId") Integer cropSeasonId,
            @Param("runStatus") String runStatus
    );

    @Query("SELECT r FROM ChecklistRun r " +
            "JOIN FETCH r.checklistRule rule " +
            "JOIN FETCH rule.farm f " +
            "LEFT JOIN FETCH r.cropSeason cs " +
            "LEFT JOIN FETCH r.block b " +
            "LEFT JOIN FETCH r.assignedTo m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE r.id = :runId")
    Optional<ChecklistRun> findByIdWithDetails(@Param("runId") Integer runId);
}

