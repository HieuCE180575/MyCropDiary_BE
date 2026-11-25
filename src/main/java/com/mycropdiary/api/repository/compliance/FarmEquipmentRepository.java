package com.mycropdiary.api.repository.compliance;

import com.mycropdiary.api.entity.compliance.FarmEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FarmEquipmentRepository extends JpaRepository<FarmEquipment, Integer> {
    List<FarmEquipment> findByFarmIdOrderByCreatedAtDesc(Integer farmId);
    boolean existsByFarmIdAndEquipmentCodeIgnoreCase(Integer farmId, String equipmentCode);
    boolean existsByFarmIdAndEquipmentCodeIgnoreCaseAndIdNot(Integer farmId, String equipmentCode, Integer id);
}
