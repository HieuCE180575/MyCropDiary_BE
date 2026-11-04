package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    List<Warehouse> findByFarmIdOrderByWarehouseNameAsc(Integer farmId);
    Optional<Warehouse> findByIdAndFarmId(Integer id, Integer farmId);
    boolean existsByFarmIdAndWarehouseNameIgnoreCase(Integer farmId, String warehouseName);
}

