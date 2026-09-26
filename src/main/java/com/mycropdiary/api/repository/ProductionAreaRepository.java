package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.ProductionArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionAreaRepository extends JpaRepository<ProductionArea, Long> {
    List<ProductionArea> findByFarmId(Long farmId);
    List<ProductionArea> findByFarmIdAndStatus(Long farmId, String status);
    Optional<ProductionArea> findByFarmIdAndAreaCode(Long farmId, String areaCode);
    boolean existsByFarmIdAndAreaCode(Long farmId, String areaCode);
}
