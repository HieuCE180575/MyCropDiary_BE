package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.WaterSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaterSourceRepository extends JpaRepository<WaterSource, Integer> {
    List<WaterSource> findByFarmIdOrderBySourceNameAsc(Integer farmId);
}

