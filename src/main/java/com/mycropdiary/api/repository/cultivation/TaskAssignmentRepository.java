package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.TaskAssignment;
import com.mycropdiary.api.entity.cultivation.TaskAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentId> {
    List<TaskAssignment> findByFarmTaskId(Integer farmTaskId);
    void deleteByFarmTaskId(Integer farmTaskId);
}

