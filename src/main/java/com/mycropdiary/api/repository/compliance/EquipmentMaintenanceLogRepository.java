package com.mycropdiary.api.repository.compliance;

import com.mycropdiary.api.entity.compliance.EquipmentMaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentMaintenanceLogRepository extends JpaRepository<EquipmentMaintenanceLog, Integer> {
    List<EquipmentMaintenanceLog> findByFarmIdOrderByActionDateDescIdDesc(Integer farmId);
    boolean existsByEquipmentId(Integer equipmentId);
}
