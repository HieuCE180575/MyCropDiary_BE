package com.mycropdiary.api.service.compliance;

import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceReqDTO;
import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceRespDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentReqDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentRespDTO;
import java.util.List;

/**
 * UC-46: Manage Farm Equipment & Maintenance Logs (Mục 4.9 VietGAP, BR-99).
 */
public interface FarmEquipmentService {
    // Danh mục thiết bị (Staff + Owner xem; chỉ Owner CUD)
    List<FarmEquipmentRespDTO> getEquipments(Integer currentUserId, Integer farmId);
    FarmEquipmentRespDTO createEquipment(Integer currentUserId, Integer farmId, FarmEquipmentReqDTO request);
    FarmEquipmentRespDTO updateEquipment(Integer currentUserId, Integer equipmentId, FarmEquipmentReqDTO request);
    void deleteEquipment(Integer currentUserId, Integer equipmentId);

    // Nhật ký vệ sinh / bảo dưỡng / hiệu chuẩn (Staff + Owner)
    List<EquipmentMaintenanceRespDTO> getMaintenanceLogs(Integer currentUserId, Integer farmId);
    EquipmentMaintenanceRespDTO recordMaintenanceLog(Integer currentUserId, Integer farmId, EquipmentMaintenanceReqDTO request);
    void deleteMaintenanceLog(Integer currentUserId, Integer maintenanceId);
}
