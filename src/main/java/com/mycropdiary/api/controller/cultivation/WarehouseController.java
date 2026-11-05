package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.warehouse.CreateInventoryReq;
import com.mycropdiary.api.dto.warehouse.CreateWarehouseReq;
import com.mycropdiary.api.dto.warehouse.InventoryItemResp;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReq;
import com.mycropdiary.api.dto.warehouse.WarehouseResp;
import com.mycropdiary.api.service.cultivation.WarehouseService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public ApiResp<List<WarehouseResp>> getWarehousesByFarm(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<WarehouseResp> list = warehouseService.getWarehousesByFarm(currentUserId, farmId);
        return ApiResp.ok("Lấy danh sách kho hàng thành công.", list);
    }

    @Operation(summary = "Tạo mới kho hàng", description = "Thêm một kho hàng mới vào hệ thống quản lý của trang trại.")
    @PostMapping("/farms/{farmId}/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<WarehouseResp> createWarehouse(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateWarehouseReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseResp response = warehouseService.createWarehouse(currentUserId, farmId, request);
        return ApiResp.ok("Tạo mới kho hàng thành công.", response);
    }

    @Operation(summary = "Xem chi tiết kho hàng", description = "Xem thông tin chi tiết một kho hàng theo ID.")
    @GetMapping("/warehouses/{warehouseId}")
    public ApiResp<WarehouseResp> getWarehouseDetails(@PathVariable Integer warehouseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseResp response = warehouseService.getWarehouseDetails(currentUserId, warehouseId);
        return ApiResp.ok("Chi tiết kho hàng.", response);
    }

    @Operation(summary = "Cập nhật thông tin kho hàng", description = "Chỉnh sửa tên, địa chỉ hoặc hình ảnh của kho hàng.")
    @PutMapping("/warehouses/{warehouseId}")
    public ApiResp<WarehouseResp> updateWarehouse(
            @PathVariable Integer warehouseId,
            @Valid @RequestBody CreateWarehouseReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        WarehouseResp response = warehouseService.updateWarehouse(currentUserId, warehouseId, request);
        return ApiResp.ok("Cập nhật kho hàng thành công.", response);
    }

    @Operation(summary = "Xóa kho hàng", description = "Xóa kho hàng và toàn bộ dữ liệu tồn kho liên kết.")
    @DeleteMapping("/warehouses/{warehouseId}")
    public ApiResp<Void> deleteWarehouse(@PathVariable Integer warehouseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        warehouseService.deleteWarehouse(currentUserId, warehouseId);
        return ApiResp.ok("Xóa kho hàng thành công.", null);
    }

    @Operation(summary = "Xem danh sách tồn kho vật tư", description = "Lấy danh sách các mặt hàng tồn kho toàn trang trại hoặc lọc theo kho cụ thể và từ khóa vật tư.")
    @GetMapping("/farms/{farmId}/inventories")
    public ApiResp<List<InventoryItemResp>> getInventoriesByFarm(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<InventoryItemResp> list = warehouseService.getInventoriesByFarm(currentUserId, farmId, warehouseId, keyword);
        return ApiResp.ok("Lấy danh sách tồn kho thành công.", list);
    }

    @Operation(summary = "Tạo mới bản ghi tồn kho vật tư", description = "Thêm một mặt hàng vật tư vào quản lý tồn kho tại kho hàng chỉ định.")
    @PostMapping("/farms/{farmId}/inventories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<InventoryItemResp> createInventory(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateInventoryReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InventoryItemResp response = warehouseService.createInventory(currentUserId, farmId, request);
        return ApiResp.ok("Thêm vật tư vào kho thành công.", response);
    }

    @Operation(summary = "Xem chi tiết tồn kho vật tư", description = "Xem thông tin chi tiết tồn kho, số lượng hiện có và mức cảnh báo đặt hàng.")
    @GetMapping("/inventories/{inventoryId}")
    public ApiResp<InventoryItemResp> getInventoryDetails(@PathVariable Integer inventoryId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InventoryItemResp response = warehouseService.getInventoryDetails(currentUserId, inventoryId);
        return ApiResp.ok("Chi tiết tồn kho vật tư.", response);
    }

    @Operation(summary = "Cập nhật số lượng tồn kho", description = "Điều chỉnh số lượng thực tế trong kho và mức cảnh báo đặt hàng lại.")
    @PutMapping("/inventories/{inventoryId}")
    public ApiResp<InventoryItemResp> updateInventory(
            @PathVariable Integer inventoryId,
            @Valid @RequestBody UpdateInventoryReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InventoryItemResp response = warehouseService.updateInventory(currentUserId, inventoryId, request);
        return ApiResp.ok("Cập nhật tồn kho thành công.", response);
    }

    @Operation(summary = "Xóa mặt hàng khỏi tồn kho", description = "Xóa bản ghi tồn kho của vật tư trong kho hàng.")
    @DeleteMapping("/inventories/{inventoryId}")
    public ApiResp<Void> deleteInventory(@PathVariable Integer inventoryId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        warehouseService.deleteInventory(currentUserId, inventoryId);
        return ApiResp.ok("Xóa bản ghi tồn kho thành công.", null);
    }
}

