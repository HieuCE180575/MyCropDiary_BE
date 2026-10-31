package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.TrainingRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Integer>, JpaSpecificationExecutor<TrainingRecord> {
    List<TrainingRecord> findByFarmIdOrderByTrainingDateDesc(Integer farmId);
    Page<TrainingRecord> findByFarmId(Integer farmId, Pageable pageable);
}

