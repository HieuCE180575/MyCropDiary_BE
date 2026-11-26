package com.mycropdiary.api.service.unit;

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
import com.mycropdiary.api.mapper.FarmEquipmentMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.compliance.EquipmentMaintenanceLogRepository;
import com.mycropdiary.api.repository.compliance.FarmEquipmentRepository;
import com.mycropdiary.api.service.impl.FarmEquipmentServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE09_EQP - FarmEquipmentServiceImpl (UC-46: Quản lý Thiết bị & Nhật ký Bảo dưỡng VietGAP Mục 4.9 / BR-99)")
public class FE09_EquipmentMaintenanceUnitTest {

    @Mock private FarmRepository farmRepository;
    @Mock private FarmEquipmentRepository equipmentRepository;
    @Mock private EquipmentMaintenanceLogRepository maintenanceLogRepository;
    @Mock private FarmAccessGuard accessGuard;
    @Spy  private FarmEquipmentMapper mapper;

    private FarmEquipmentServiceImpl service;

    private Farm farm;
    private Farm otherFarm;
    private FarmEquipment sprayer;

    @BeforeEach
    void setUp() {
        service = new FarmEquipmentServiceImpl(farmRepository, equipmentRepository, maintenanceLogRepository, accessGuard, mapper);

        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Nông trại VietGAP");

        otherFarm = new Farm();
        otherFarm.setId(2);
        otherFarm.setFarmName("Nông trại Khác");

        sprayer = new FarmEquipment();
        sprayer.setId(10);
        sprayer.setFarm(farm);
        sprayer.setEquipmentCode("EQ-001");
        sprayer.setEquipmentName("Bình phun thuốc 16L");
        sprayer.setEquipmentType("sprayer");
        sprayer.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("FE09_EQP_UTC01 [Normal] - Owner tạo thiết bị mới với mã duy nhất -> Mặc định trạng thái ACTIVE")
    void UTC01_createEquipment_validUniqueCode_savesActive() {
        FarmEquipmentReqDTO req = new FarmEquipmentReqDTO("EQ-002", "Máy đo pH đất", "measuring_tool", null, "Hiệu chuẩn định kỳ");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(equipmentRepository.existsByFarmIdAndEquipmentCodeIgnoreCase(1, "EQ-002")).thenReturn(false);
        when(equipmentRepository.save(any(FarmEquipment.class))).thenAnswer(i -> {
            FarmEquipment e = i.getArgument(0);
            e.setId(11);
            return e;
        });

        FarmEquipmentRespDTO resp = service.createEquipment(100, 1, req);

        assertNotNull(resp);
        assertEquals("EQ-002", resp.equipmentCode());
        assertEquals("ACTIVE", resp.status());
        verify(accessGuard).requireFarmOwner(100, 1);
    }

    @Test
    @DisplayName("FE09_EQP_UTC02 [Abnormal] - Tạo thiết bị trùng mã trong cùng trang trại -> Ném ResourceConflictException")
    void UTC02_createEquipment_duplicateCode_throwsConflict() {
        FarmEquipmentReqDTO req = new FarmEquipmentReqDTO("EQ-001", "Bình phun khác", "sprayer", "ACTIVE", null);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(equipmentRepository.existsByFarmIdAndEquipmentCodeIgnoreCase(1, "EQ-001")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.createEquipment(100, 1, req));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE09_EQP_UTC03 [Normal] - Owner cập nhật thông tin & trạng thái thiết bị hợp lệ -> Lưu thành công")
    void UTC03_updateEquipment_validData_updatesSuccessfully() {
        FarmEquipmentReqDTO req = new FarmEquipmentReqDTO("EQ-001", "Bình phun thuốc điện 20L", "sprayer", "maintenance", "Thay béc phun");
        when(equipmentRepository.findById(10)).thenReturn(Optional.of(sprayer));
        when(equipmentRepository.existsByFarmIdAndEquipmentCodeIgnoreCaseAndIdNot(1, "EQ-001", 10)).thenReturn(false);
        when(equipmentRepository.save(any(FarmEquipment.class))).thenAnswer(i -> i.getArgument(0));

        FarmEquipmentRespDTO resp = service.updateEquipment(100, 10, req);

        assertEquals("Bình phun thuốc điện 20L", resp.equipmentName());
        assertEquals("MAINTENANCE", resp.status());
    }

    @Test
    @DisplayName("FE09_EQP_UTC04 [Boundary] - Xóa thiết bị đã có nhật ký bảo dưỡng (BR-20) -> Chuyển sang INACTIVE thay vì xóa cứng")
    void UTC04_deleteEquipment_referencedByMaintenanceLogs_softDeactivatesPerBR20() {
        when(equipmentRepository.findById(10)).thenReturn(Optional.of(sprayer));
        when(maintenanceLogRepository.existsByEquipmentId(10)).thenReturn(true);

        service.deleteEquipment(100, 10);

        assertEquals("INACTIVE", sprayer.getStatus());
        verify(equipmentRepository).save(sprayer);
        verify(equipmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("FE09_EQP_UTC05 [Normal] - Xóa thiết bị chưa từng phát sinh nhật ký bảo dưỡng -> Xóa cứng thành công")
    void UTC05_deleteEquipment_unreferenced_hardDeletes() {
        when(equipmentRepository.findById(10)).thenReturn(Optional.of(sprayer));
        when(maintenanceLogRepository.existsByEquipmentId(10)).thenReturn(false);

        service.deleteEquipment(100, 10);

        verify(equipmentRepository).delete(sprayer);
    }

    @Test
    @DisplayName("FE09_EQP_UTC06 [Normal] - Staff/Owner ghi nhận vệ sinh bình phun (rinse_cleaning + holding_tank) -> Tự động ánh xạ mã thiết bị & lưu an toàn")
    void UTC06_recordMaintenanceLog_validEquipmentAndHandling_savesSuccessfully() {
        EquipmentMaintenanceReqDTO req = new EquipmentMaintenanceReqDTO(
                10, null, null, "rinse_cleaning", LocalDate.now(), "Nguyễn Văn Tèo",
                "holding_tank", null, "Súc rửa 3 lần đạt chuẩn VietGAP"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(equipmentRepository.findById(10)).thenReturn(Optional.of(sprayer));
        when(maintenanceLogRepository.save(any(EquipmentMaintenanceLog.class))).thenAnswer(i -> {
            EquipmentMaintenanceLog log = i.getArgument(0);
            log.setId(50);
            return log;
        });

        EquipmentMaintenanceRespDTO resp = service.recordMaintenanceLog(200, 1, req);

        assertNotNull(resp);
        assertEquals("EQ-001", resp.equipmentCode());
        assertEquals("Bình phun thuốc 16L", resp.equipmentName());
        assertEquals(Boolean.TRUE, resp.isSafeForUse());
        verify(accessGuard).requireFarmAccess(200, 1);
    }

    @Test
    @DisplayName("FE09_EQP_UTC07 [Abnormal] - Ghi nhận nhật ký với actionType không hợp lệ -> Ném BadRequestException")
    void UTC07_recordMaintenanceLog_invalidActionType_throwsBadRequest() {
        EquipmentMaintenanceReqDTO req = new EquipmentMaintenanceReqDTO(
                10, null, null, "unknown_action", LocalDate.now(), "Kỹ thuật viên", "holding_tank", true, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.recordMaintenanceLog(200, 1, req));
    }

    @Test
    @DisplayName("FE09_EQP_UTC08 [Abnormal] - Ghi nhận nhật ký với phương pháp xử lý nước rửa không hợp lệ -> Ném BadRequestException")
    void UTC08_recordMaintenanceLog_invalidRinseHandling_throwsBadRequest() {
        EquipmentMaintenanceReqDTO req = new EquipmentMaintenanceReqDTO(
                10, null, null, "rinse_cleaning", LocalDate.now(), "Kỹ thuật viên", "dump_to_river", true, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.recordMaintenanceLog(200, 1, req));
    }

    @Test
    @DisplayName("FE09_EQP_UTC09 [Abnormal] - Ghi nhận bảo dưỡng bằng thiết bị thuộc trang trại khác (Cross-Farm) -> Ném BusinessRuleException")
    void UTC09_recordMaintenanceLog_equipmentFromAnotherFarm_throwsBusinessRule() {
        FarmEquipment foreignEquip = new FarmEquipment();
        foreignEquip.setId(99);
        foreignEquip.setFarm(otherFarm);

        EquipmentMaintenanceReqDTO req = new EquipmentMaintenanceReqDTO(
                99, null, null, "calibration", LocalDate.now(), "Kỹ thuật viên", "special_pit", true, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(equipmentRepository.findById(99)).thenReturn(Optional.of(foreignEquip));

        assertThrows(BusinessRuleException.class, () -> service.recordMaintenanceLog(200, 1, req));
    }

    @Test
    @DisplayName("FE09_EQP_UTC10 [Boundary] - Thiếu cả equipmentId và equipmentName bị chặn BadRequest; xóa nhật ký yêu cầu quyền Owner")
    void UTC10_recordMaintenanceMissingNameAndDeleteRequiresOwner() {
        EquipmentMaintenanceReqDTO missingReq = new EquipmentMaintenanceReqDTO(
                null, null, "   ", "maintenance_repair", LocalDate.now(), "Kỹ thuật viên", "diluted_field", true, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        assertThrows(BadRequestException.class, () -> service.recordMaintenanceLog(200, 1, missingReq));

        EquipmentMaintenanceLog existingLog = new EquipmentMaintenanceLog();
        existingLog.setId(50);
        existingLog.setFarm(farm);
        when(maintenanceLogRepository.findById(50)).thenReturn(Optional.of(existingLog));

        service.deleteMaintenanceLog(100, 50);
        verify(accessGuard).requireFarmOwner(100, 1);
        verify(maintenanceLogRepository).delete(existingLog);
    }
}
