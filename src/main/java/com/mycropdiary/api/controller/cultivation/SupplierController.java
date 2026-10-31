package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.procurement.CreateSupplierReq;
import com.mycropdiary.api.dto.procurement.SupplierResp;
import com.mycropdiary.api.dto.procurement.UpdateSupplierReq;
import com.mycropdiary.api.service.cultivation.SupplierService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-35: Supplier Management", description = "Quản lý danh bạ nhà cung cấp vật tư & giống nông nghiệp (Supplier)")
@RestController
@RequestMapping("/api/v1")
public class SupplierController {

    private final SupplierService supplierService;
    private final SecurityUtils securityUtils;

    public SupplierController(SupplierService supplierService, SecurityUtils securityUtils) {
        this.supplierService = supplierService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách nhà cung cấp của nông trại (View Supplier List)")
    @GetMapping("/farms/{farmId}/suppliers")
    public ApiResp<List<SupplierResp>> getSuppliers(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<SupplierResp> list = supplierService.getSuppliers(currentUserId, farmId, keyword);
        return ApiResp.ok("Danh sách nhà cung cấp", list);
    }

    @Operation(summary = "Xem chi tiết nhà cung cấp (View Supplier Details)")
    @GetMapping("/suppliers/{supplierId}")
    public ApiResp<SupplierResp> getSupplierDetails(@PathVariable Integer supplierId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SupplierResp supplier = supplierService.getSupplierDetails(currentUserId, supplierId);
        return ApiResp.ok("Chi tiết nhà cung cấp", supplier);
    }

    @Operation(summary = "Tạo mới nhà cung cấp (Create Supplier)")
    @PostMapping("/farms/{farmId}/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<SupplierResp> createSupplier(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateSupplierReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SupplierResp created = supplierService.createSupplier(currentUserId, farmId, request);
        return ApiResp.ok("Tạo mới nhà cung cấp thành công", created);
    }

    @Operation(summary = "Cập nhật thông tin nhà cung cấp (Update Supplier)")
    @PutMapping("/suppliers/{supplierId}")
    public ApiResp<SupplierResp> updateSupplier(
            @PathVariable Integer supplierId,
            @Valid @RequestBody UpdateSupplierReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SupplierResp updated = supplierService.updateSupplier(currentUserId, supplierId, request);
        return ApiResp.ok("Cập nhật nhà cung cấp thành công", updated);
    }

    @Operation(summary = "Xóa nhà cung cấp đủ điều kiện (Delete Eligible Supplier)")
    @DeleteMapping("/suppliers/{supplierId}")
    public ApiResp<Void> deleteSupplier(@PathVariable Integer supplierId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        supplierService.deleteSupplier(currentUserId, supplierId);
        return ApiResp.ok("Đã xóa nhà cung cấp", null);
    }
}
