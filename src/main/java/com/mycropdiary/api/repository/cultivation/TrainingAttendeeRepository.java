package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.TrainingAttendee;
import com.mycropdiary.api.entity.cultivation.TrainingAttendeeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingAttendeeRepository extends JpaRepository<TrainingAttendee, TrainingAttendeeId> {
    List<TrainingAttendee> findByIdTrainingRecordId(Integer trainingRecordId);
    void deleteByIdTrainingRecordId(Integer trainingRecordId);
}

