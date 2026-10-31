package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.procurement.CreateInputPurchaseReq;
import com.mycropdiary.api.dto.procurement.InputPurchaseResp;
import com.mycropdiary.api.dto.procurement.LinkWarehouseReceiptReq;
import com.mycropdiary.api.dto.procurement.UpdateInputPurchaseReq;
import com.mycropdiary.api.service.cultivation.InputPurchaseService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "UC-36: Input Purchases & Procurement", description = "Quản lý mua sắm vật tư nông nghiệp & Nhập kho (Input Purchases)")
@RestController
@RequestMapping("/api/v1")
public class InputPurchaseController {

    private final InputPurchaseService purchaseService;
    private final SecurityUtils securityUtils;

    public InputPurchaseController(InputPurchaseService purchaseService, SecurityUtils securityUtils) {
        this.purchaseService = purchaseService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách phiếu mua vật tư của trang trại (View Input Purchase List)")
    @GetMapping("/farms/{farmId}/input-purchases")
    public ApiResp<List<InputPurchaseResp>> getInputPurchases(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<InputPurchaseResp> list = purchaseService.getInputPurchases(currentUserId, farmId);
        return ApiResp.ok("Danh sách phiếu mua vật tư", list);
    }

    @Operation(summary = "Xem chi tiết phiếu mua vật tư (View Input Purchase Details)")
    @GetMapping("/input-purchases/{purchaseId}")
    public ApiResp<InputPurchaseResp> getInputPurchaseDetails(@PathVariable Integer purchaseId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        InputPurchaseResp details = purchaseService.getInputPurchaseDetails(currentUserId, purchaseId);
        return ApiResp.ok("Chi tiết phiếu mua vật tư", details);
    }

    @Operation(summary = "Tìm kiếm & lọc phiếu mua vật tư (Search Input Purchases)")
    @GetMapping("/farms/{farmId}/input-purchases/search")
    public ApiResp<List<InputPurchaseResp>> searchInputPurchases(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<InputPurchaseResp> list = purchaseService.searchInputPurchases(currentUserId, farmId, keyword, supplierId, fromDate, toDate);
        return ApiResp.ok("Kết quả tìm kiếm phiếu mua vật tư", list);
    }

    @Operation(summary = "Ghi nhận phiếu mua vật tư mới (Record Input Purchase)")
    @PostMapping("/farms/{farmId}/input-purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<InputPurchaseResp> recordInputPurchase(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateInputPurchaseReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        InputPurchaseResp created = purchaseService.recordInputPurchase(currentUserId, farmId, request);
        return ApiResp.ok("Ghi nhận phiếu mua vật tư thành công", created);
    }

    @Operation(summary = "Cập nhật phiếu mua vật tư (Update Input Purchase)")
    @PutMapping("/input-purchases/{purchaseId}")
    public ApiResp<InputPurchaseResp> updateInputPurchase(
            @PathVariable Integer purchaseId,
            @Valid @RequestBody UpdateInputPurchaseReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        InputPurchaseResp updated = purchaseService.updateInputPurchase(currentUserId, purchaseId, request);
        return ApiResp.ok("Cập nhật phiếu mua vật tư thành công", updated);
    }

    @Operation(summary = "Xóa phiếu mua vật tư đủ điều kiện (Delete Eligible Input Purchase)")
    @DeleteMapping("/input-purchases/{purchaseId}")
    public ApiResp<Void> deleteInputPurchase(@PathVariable Integer purchaseId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        purchaseService.deleteInputPurchase(currentUserId, purchaseId);
        return ApiResp.ok("Đã xóa phiếu mua vật tư", null);
    }

    @Operation(summary = "Liên kết phiếu mua với phiếu nhập kho (Link Input Purchase to Warehouse Receipt)")
    @PostMapping("/input-purchases/{purchaseId}/link-warehouse-receipt")
    public ApiResp<InputPurchaseResp> linkWarehouseReceipt(
            @PathVariable Integer purchaseId,
            @Valid @RequestBody LinkWarehouseReceiptReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        InputPurchaseResp response = purchaseService.linkWarehouseReceipt(currentUserId, purchaseId, request);
        return ApiResp.ok("Liên kết phiếu nhập kho thành công", response);
    }
}
