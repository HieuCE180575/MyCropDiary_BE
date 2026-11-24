package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.BlockCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockConditionRepository extends JpaRepository<BlockCondition, Integer> {
    List<BlockCondition> findByBlockIdOrderByObservedAtDesc(Integer blockId);
    List<BlockCondition> findByBlockZoneFarmIdOrderByObservedAtDesc(Integer farmId);
}
