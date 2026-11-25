package com.mycropdiary.api.controller.compliance;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceReqDTO;
import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceRespDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentReqDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentRespDTO;
import com.mycropdiary.api.service.compliance.FarmEquipmentService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-46: Farm Equipment & Maintenance (Mục 4.9 VietGAP)", description = "Quản lý danh mục thiết bị (Owner CUD) và nhật ký vệ sinh/bảo dưỡng bình phun (Staff + Owner)")
@SecurityRequirement(name = "BearerAuthentication")
@RestController
@RequestMapping("/api/v1")
public class FarmEquipmentController {

    private final FarmEquipmentService equipmentService;
    private final SecurityUtils securityUtils;

    public FarmEquipmentController(FarmEquipmentService equipmentService, SecurityUtils securityUtils) {
        this.equipmentService = equipmentService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "UC-46: Xem danh sách thiết bị của trang trại (Staff + Owner)")
    @GetMapping("/farms/{farmId}/equipments")
    public ApiResp<List<FarmEquipmentRespDTO>> getEquipments(@PathVariable Integer farmId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Danh sách thiết bị trang trại", equipmentService.getEquipments(userId, farmId));
    }

    @Operation(summary = "UC-46 (Owner): Thêm thiết bị mới vào danh mục")
    @PostMapping("/farms/{farmId}/equipments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmEquipmentRespDTO> createEquipment(
            @PathVariable Integer farmId,
            @Valid @RequestBody FarmEquipmentReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Thêm thiết bị thành công", equipmentService.createEquipment(userId, farmId, request));
    }

    @Operation(summary = "UC-46 (Owner): Cập nhật thông tin thiết bị")
    @PutMapping("/equipments/{equipmentId}")
    public ApiResp<FarmEquipmentRespDTO> updateEquipment(
            @PathVariable Integer equipmentId,
            @Valid @RequestBody FarmEquipmentReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Cập nhật thiết bị thành công", equipmentService.updateEquipment(userId, equipmentId, request));
    }

    @Operation(summary = "UC-46 (Owner): Xóa hoặc ngừng hoạt động thiết bị")
    @DeleteMapping("/equipments/{equipmentId}")
    public ApiResp<Void> deleteEquipment(@PathVariable Integer equipmentId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        equipmentService.deleteEquipment(userId, equipmentId);
        return ApiResp.ok("Đã xóa / lưu trữ thiết bị", null);
    }

    @Operation(summary = "UC-46: Xem lịch sử vệ sinh, bảo dưỡng, hiệu chuẩn thiết bị (Staff + Owner)")
    @GetMapping("/farms/{farmId}/equipment-maintenances")
    public ApiResp<List<EquipmentMaintenanceRespDTO>> getMaintenanceLogs(@PathVariable Integer farmId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Lịch sử bảo dưỡng & vệ sinh thiết bị", equipmentService.getMaintenanceLogs(userId, farmId));
    }

    @Operation(summary = "UC-46: Ghi nhận vệ sinh, súc rửa bình phun, bảo dưỡng, hiệu chuẩn (Staff + Owner)")
    @PostMapping("/farms/{farmId}/equipment-maintenances")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<EquipmentMaintenanceRespDTO> recordMaintenanceLog(
            @PathVariable Integer farmId,
            @Valid @RequestBody EquipmentMaintenanceReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Ghi nhận nhật ký thiết bị thành công", equipmentService.recordMaintenanceLog(userId, farmId, request));
    }

    @Operation(summary = "UC-46 (Owner): Xóa nhật ký bảo dưỡng thiết bị")
    @DeleteMapping("/equipment-maintenances/{maintenanceId}")
    public ApiResp<Void> deleteMaintenanceLog(@PathVariable Integer maintenanceId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        equipmentService.deleteMaintenanceLog(userId, maintenanceId);
        return ApiResp.ok("Đã xóa nhật ký bảo dưỡng", null);
    }
}
