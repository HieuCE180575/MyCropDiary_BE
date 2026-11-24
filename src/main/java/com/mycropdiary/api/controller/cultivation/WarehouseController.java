package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.warehouse.CreateWarehouseReqDTO;
import com.mycropdiary.api.dto.warehouse.UpdateWarehouseReqDTO;
import com.mycropdiary.api.dto.warehouse.WarehouseOverviewRespDTO;
import com.mycropdiary.api.dto.warehouse.WarehouseRespDTO;
import com.mycropdiary.api.service.cultivation.WarehouseService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "12. Quản lý kho & Tồn kho vật tư (Warehouse & Inventory)", description = "APIs quản lý danh sách kho bãi, theo dõi tồn kho và định mức tồn trữ vật tư nông nghiệp")
@SecurityRequirement(name = "BearerAuthentication")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final SecurityUtils securityUtils;

    public WarehouseController(WarehouseService warehouseService, SecurityUtils securityUtils) {
        this.warehouseService = warehouseService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách kho hàng", description = "Lấy danh sách các kho vật tư, kho bảo quản nông sản của trang trại.")
    @GetMapping("/farms/{farmId}/warehouses")
    public ApiResp<List<WarehouseRespDTO>> getWarehousesByFarm(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<WarehouseRespDTO> list = warehouseService.getWarehousesByFarm(currentUserId, farmId);
        return ApiResp.ok("Lấy danh sách kho hàng thành công.", list);
    }

    @Operation(summary = "Tạo mới kho hàng", description = "Thêm một kho hàng mới vào hệ thống quản lý của trang trại.")
    @PostMapping("/farms/{farmId}/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<WarehouseRespDTO> createWarehouse(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateWarehouseReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseRespDTO response = warehouseService.createWarehouse(currentUserId, farmId, request);
        return ApiResp.ok("Tạo mới kho hàng thành công.", response);
    }

    @Operation(summary = "Xem chi tiết kho hàng", description = "Xem thông tin chi tiết một kho hàng theo ID.")
    @GetMapping("/warehouses/{warehouseId}")
    public ApiResp<WarehouseRespDTO> getWarehouseDetails(@PathVariable Integer warehouseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseRespDTO response = warehouseService.getWarehouseDetails(currentUserId, warehouseId);
        return ApiResp.ok("Chi tiết kho hàng.", response);
    }

    @Operation(summary = "Cập nhật thông tin kho hàng", description = "Chỉnh sửa tên, địa chỉ hoặc hình ảnh của kho hàng.")
    @PutMapping("/warehouses/{warehouseId}")
    public ApiResp<WarehouseRespDTO> updateWarehouse(
            @PathVariable Integer warehouseId,
            @Valid @RequestBody UpdateWarehouseReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseRespDTO response = warehouseService.updateWarehouse(currentUserId, warehouseId, request);
        return ApiResp.ok("Cập nhật kho hàng thành công.", response);
    }

    @Operation(summary = "Xóa kho hàng", description = "Xóa kho hàng và toàn bộ dữ liệu tồn kho liên kết.")
    @DeleteMapping("/warehouses/{warehouseId}")
    public ApiResp<Void> deleteWarehouse(@PathVariable Integer warehouseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        warehouseService.deleteWarehouse(currentUserId, warehouseId);
        return ApiResp.ok("Xóa kho hàng thành công.", null);
    }

    @GetMapping("/warehouses/{warehouseId}/overview")
    public ResponseEntity<ApiResp<WarehouseOverviewRespDTO>> getWarehouseOverview(
            @PathVariable Integer warehouseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseOverviewRespDTO response = warehouseService.getWarehouseOverview(
                currentUserId,
                warehouseId);

        return ResponseEntity.ok(ApiResp.ok("Warehouse details", response));
    }
}
