package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.warehouse.InventoryRespDTO;
import com.mycropdiary.api.dto.warehouse.StockInReqDTO;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReqDTO;
import com.mycropdiary.api.service.cultivation.InventoryService;
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
@Tag(name = "12.2. Quản lý tồn kho vật tư (Inventory)", description = "APIs theo dõi tồn kho, nhập kho và cập nhật định mức tồn trữ vật tư nông nghiệp")
@SecurityRequirement(name = "BearerAuthentication")
public class InventoryController {

    private final InventoryService inventoryService;
    private final SecurityUtils securityUtils;

    public InventoryController(InventoryService inventoryService, SecurityUtils securityUtils) {
        this.inventoryService = inventoryService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách tồn kho vật tư", description = "Lấy danh sách các mặt hàng tồn kho toàn trang trại hoặc lọc theo kho cụ thể và từ khóa vật tư.")
    @GetMapping("/farms/{farmId}/inventories")
    public ApiResp<List<InventoryRespDTO>> getInventoriesByFarm(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<InventoryRespDTO> list = inventoryService.getInventoriesByFarm(currentUserId, farmId, warehouseId, keyword);
        return ApiResp.ok("Lấy danh sách tồn kho thành công.", list);
    }

    @Operation(summary = "Tạo mới bản ghi tồn kho vật tư", description = "Thêm một mặt hàng vật tư vào quản lý tồn kho tại kho hàng chỉ định.")
    @PostMapping("/farms/{farmId}/inventories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<InventoryRespDTO> createInventory(
            @PathVariable Integer farmId,
            @Valid @RequestBody StockInReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InventoryRespDTO response = inventoryService.createInventory(currentUserId, farmId, request);
        return ApiResp.ok("Thêm vật tư vào kho thành công.", response);
    }

    @Operation(summary = "Xem chi tiết tồn kho vật tư", description = "Xem thông tin chi tiết tồn kho, số lượng hiện có và mức cảnh báo đặt hàng.")
    @GetMapping("/inventories/{inventoryId}")
    public ApiResp<InventoryRespDTO> getInventoryDetails(@PathVariable Integer inventoryId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InventoryRespDTO response = inventoryService.getInventoryDetails(currentUserId, inventoryId);
        return ApiResp.ok("Chi tiết tồn kho vật tư.", response);
    }

    @Operation(summary = "Cập nhật số lượng tồn kho", description = "Điều chỉnh số lượng thực tế trong kho và mức cảnh báo đặt hàng lại.")
    @PutMapping("/inventories/{inventoryId}")
    public ApiResp<InventoryRespDTO> updateInventory(
            @PathVariable Integer inventoryId,
            @Valid @RequestBody UpdateInventoryReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InventoryRespDTO response = inventoryService.updateInventory(currentUserId, inventoryId, request);
        return ApiResp.ok("Cập nhật tồn kho thành công.", response);
    }

    @Operation(summary = "Xóa mặt hàng khỏi tồn kho", description = "Xóa bản ghi tồn kho của vật tư trong kho hàng.")
    @DeleteMapping("/inventories/{inventoryId}")
    public ApiResp<Void> deleteInventory(@PathVariable Integer inventoryId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        inventoryService.deleteInventory(currentUserId, inventoryId);
        return ApiResp.ok("Xóa bản ghi tồn kho thành công.", null);
    }
}
