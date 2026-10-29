package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.FarmTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmTaskRepository extends JpaRepository<FarmTask, Integer>, JpaSpecificationExecutor<FarmTask> {
    List<FarmTask> findByFarmId(Integer farmId);
    Page<FarmTask> findByFarmId(Integer farmId, Pageable pageable);
    Optional<FarmTask> findByIdAndFarmId(Integer id, Integer farmId);
    boolean existsByFarmId(Integer farmId);
    long countByFarmId(Integer farmId);
}

