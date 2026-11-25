package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceReqDTO;
import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceRespDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentReqDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.compliance.EquipmentMaintenanceLog;
import com.mycropdiary.api.entity.compliance.FarmEquipment;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmEquipmentMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.compliance.EquipmentMaintenanceLogRepository;
import com.mycropdiary.api.repository.compliance.FarmEquipmentRepository;
import com.mycropdiary.api.service.compliance.FarmEquipmentService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service nghiệp vụ cho UC-46: Manage Farm Equipment & Maintenance Logs (Mục 4.9 VietGAP, BR-99).
 */
@Service
@Transactional
public class FarmEquipmentServiceImpl implements FarmEquipmentService {

    private static final Set<String> VALID_ACTION_TYPES = Set.of("rinse_cleaning", "maintenance_repair", "calibration");
    private static final Set<String> VALID_RINSE_HANDLING = Set.of("holding_tank", "diluted_field", "special_pit");

    private final FarmRepository farmRepository;
    private final FarmEquipmentRepository equipmentRepository;
    private final EquipmentMaintenanceLogRepository maintenanceLogRepository;
    private final FarmAccessGuard accessGuard;
    private final FarmEquipmentMapper mapper;

    public FarmEquipmentServiceImpl(FarmRepository farmRepository,
                                    FarmEquipmentRepository equipmentRepository,
                                    EquipmentMaintenanceLogRepository maintenanceLogRepository,
                                    FarmAccessGuard accessGuard,
                                    FarmEquipmentMapper mapper) {
        this.farmRepository = farmRepository;
        this.equipmentRepository = equipmentRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
        this.accessGuard = accessGuard;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmEquipmentRespDTO> getEquipments(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        return equipmentRepository.findByFarmIdOrderByCreatedAtDesc(farmId)
                .stream()
                .map(mapper::toEquipmentResponse)
                .toList();
    }

    @Override
    public FarmEquipmentRespDTO createEquipment(Integer currentUserId, Integer farmId, FarmEquipmentReqDTO request) {
        // UC-46 Owner-only CUD
        accessGuard.requireFarmOwner(currentUserId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID=" + farmId));

        String code = request.equipmentCode().trim();
        if (equipmentRepository.existsByFarmIdAndEquipmentCodeIgnoreCase(farmId, code)) {
            throw new ResourceConflictException("Mã thiết bị '" + code + "' đã tồn tại trong trang trại này.");
        }

        FarmEquipment equipment = new FarmEquipment();
        equipment.setFarm(farm);
        equipment.setEquipmentCode(code);
        equipment.setEquipmentName(request.equipmentName().trim());
        equipment.setEquipmentType(request.equipmentType().trim());
        equipment.setStatus(request.status() != null && !request.status().isBlank() ? request.status().trim().toUpperCase() : "ACTIVE");
        equipment.setNotes(request.notes());

        return mapper.toEquipmentResponse(equipmentRepository.save(equipment));
    }

    @Override
    public FarmEquipmentRespDTO updateEquipment(Integer currentUserId, Integer equipmentId, FarmEquipmentReqDTO request) {
        FarmEquipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị ID=" + equipmentId));
        Integer farmId = equipment.getFarm().getId();
        accessGuard.requireFarmOwner(currentUserId, farmId);

        String code = request.equipmentCode().trim();
        if (equipmentRepository.existsByFarmIdAndEquipmentCodeIgnoreCaseAndIdNot(farmId, code, equipmentId)) {
            throw new ResourceConflictException("Mã thiết bị '" + code + "' đã tồn tại trong trang trại này.");
        }

        equipment.setEquipmentCode(code);
        equipment.setEquipmentName(request.equipmentName().trim());
        equipment.setEquipmentType(request.equipmentType().trim());
        if (request.status() != null && !request.status().isBlank()) {
            equipment.setStatus(request.status().trim().toUpperCase());
        }
        equipment.setNotes(request.notes());

        return mapper.toEquipmentResponse(equipmentRepository.save(equipment));
    }

    @Override
    public void deleteEquipment(Integer currentUserId, Integer equipmentId) {
        FarmEquipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị ID=" + equipmentId));
        accessGuard.requireFarmOwner(currentUserId, equipment.getFarm().getId());

        if (maintenanceLogRepository.existsByEquipmentId(equipmentId)) {
            // BR-20: Referenced master data is deactivated instead of hard-deleted
            equipment.setStatus("INACTIVE");
            equipmentRepository.save(equipment);
            return;
        }
        equipmentRepository.delete(equipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentMaintenanceRespDTO> getMaintenanceLogs(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        return maintenanceLogRepository.findByFarmIdOrderByActionDateDescIdDesc(farmId)
                .stream()
                .map(mapper::toMaintenanceResponse)
                .toList();
    }

    @Override
    public EquipmentMaintenanceRespDTO recordMaintenanceLog(Integer currentUserId, Integer farmId, EquipmentMaintenanceReqDTO request) {
        // UC-46: Cả Staff và Owner đều có quyền ghi nhận vệ sinh/bảo dưỡng thiết bị
        accessGuard.requireFarmAccess(currentUserId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID=" + farmId));

        String actionType = request.actionType().trim().toLowerCase();
        if (!VALID_ACTION_TYPES.contains(actionType)) {
            throw new BadRequestException("Loại hoạt động thiết bị không hợp lệ (chấp nhận: rinse_cleaning, maintenance_repair, calibration).");
        }
        String rinseHandling = request.rinseWaterHandling().trim().toLowerCase();
        if (!VALID_RINSE_HANDLING.contains(rinseHandling)) {
            throw new BadRequestException("Phương pháp xử lý nước rửa không hợp lệ (chấp nhận: holding_tank, diluted_field, special_pit).");
        }

        FarmEquipment equipment = null;
        String resolvedName = request.equipmentName() != null ? request.equipmentName().trim() : "";
        String resolvedCode = request.equipmentCode() != null ? request.equipmentCode().trim() : null;

        if (request.equipmentId() != null) {
            equipment = equipmentRepository.findById(request.equipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị ID=" + request.equipmentId()));
            if (!equipment.getFarm().getId().equals(farmId)) {
                throw new BusinessRuleException("Thiết bị không thuộc trang trại hiện tại.");
            }
            resolvedName = equipment.getEquipmentName();
            resolvedCode = equipment.getEquipmentCode();
        } else if (resolvedName.isBlank()) {
            throw new BadRequestException("Vui lòng chọn thiết bị từ danh mục hoặc nhập tên thiết bị.");
        }

        EquipmentMaintenanceLog log = new EquipmentMaintenanceLog();
        log.setFarm(farm);
        log.setEquipment(equipment);
        log.setEquipmentName(resolvedName);
        log.setEquipmentCode(resolvedCode);
        log.setActionType(actionType);
        log.setActionDate(request.actionDate());
        log.setPerformerName(request.performerName().trim());
        log.setRinseWaterHandling(rinseHandling);
        log.setIsSafeForUse(request.isSafeForUse() != null ? request.isSafeForUse() : true);
        log.setNotes(request.notes());

        return mapper.toMaintenanceResponse(maintenanceLogRepository.save(log));
    }

    @Override
    public void deleteMaintenanceLog(Integer currentUserId, Integer maintenanceId) {
        EquipmentMaintenanceLog log = maintenanceLogRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký bảo dưỡng ID=" + maintenanceId));
        accessGuard.requireFarmOwner(currentUserId, log.getFarm().getId());
        maintenanceLogRepository.delete(log);
    }
}
