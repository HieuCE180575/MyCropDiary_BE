package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    List<Inventory> findByWarehouseId(Integer warehouseId);
    Optional<Inventory> findByWarehouseIdAndMaterialId(Integer warehouseId, Integer materialId);

    @Query("SELECT i FROM Inventory i WHERE i.warehouse.farm.id = :farmId ORDER BY i.warehouse.warehouseName ASC, i.material.materialName ASC")
    List<Inventory> findByFarmId(@Param("farmId") Integer farmId);
}

