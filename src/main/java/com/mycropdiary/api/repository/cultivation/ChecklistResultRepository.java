package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.ChecklistResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistResultRepository extends JpaRepository<ChecklistResult, Integer> {

    @Query("SELECT res FROM ChecklistResult res " +
            "LEFT JOIN FETCH res.checkedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE res.checklistRun.id = :runId " +
            "ORDER BY res.checkedAt ASC")
    List<ChecklistResult> findByRunIdWithDetails(@Param("runId") Integer runId);
}

