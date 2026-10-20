package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.BlockConditionObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockConditionObservationRepository extends JpaRepository<BlockConditionObservation, Integer> {
    List<BlockConditionObservation> findByBlockIdOrderByObservedAtDesc(Integer blockId);
    List<BlockConditionObservation> findByBlockZoneFarmIdOrderByObservedAtDesc(Integer farmId);
}
