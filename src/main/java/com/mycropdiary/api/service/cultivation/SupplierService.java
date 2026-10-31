package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.procurement.CreateSupplierReq;
import com.mycropdiary.api.dto.procurement.SupplierResp;
import com.mycropdiary.api.dto.procurement.UpdateSupplierReq;

import java.util.List;

public interface SupplierService {
    /**
     * View Supplier List: Xem danh sách nhà cung cấp của trang trại
     */
    List<SupplierResp> getSuppliers(Integer currentUserId, Integer farmId, String keyword);

    /**
     * View Supplier Details: Xem thông tin chi tiết nhà cung cấp
     */
    SupplierResp getSupplierDetails(Integer currentUserId, Integer supplierId);

    /**
     * Create Supplier: Thêm mới nhà cung cấp
     */
    SupplierResp createSupplier(Integer currentUserId, Integer farmId, CreateSupplierReq request);

    /**
     * Update Supplier: Chỉnh sửa thông tin nhà cung cấp
     */
    SupplierResp updateSupplier(Integer currentUserId, Integer supplierId, UpdateSupplierReq request);

    /**
     * Delete Eligible Supplier: Xóa nhà cung cấp hợp lệ (chưa phát sinh hóa đơn mua hàng)
     */
    void deleteSupplier(Integer currentUserId, Integer supplierId);
}
