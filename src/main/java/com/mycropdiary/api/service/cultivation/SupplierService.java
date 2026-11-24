package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.procurement.CreateSupplierReqDTO;
import com.mycropdiary.api.dto.procurement.SupplierRespDTO;
import com.mycropdiary.api.dto.procurement.UpdateSupplierReqDTO;

import java.util.List;

public interface SupplierService {
    /**
     * View Supplier List: Xem danh sách nhà cung cấp của trang trại
     */
    List<SupplierRespDTO> getSuppliers(Integer currentUserId, Integer farmId, String keyword);

    /**
     * View Supplier Details: Xem thông tin chi tiết nhà cung cấp
     */
    SupplierRespDTO getSupplierDetails(Integer currentUserId, Integer supplierId);

    /**
     * Create Supplier: Thêm mới nhà cung cấp
     */
    SupplierRespDTO createSupplier(Integer currentUserId, Integer farmId, CreateSupplierReqDTO request);

    /**
     * Update Supplier: Chỉnh sửa thông tin nhà cung cấp
     */
    SupplierRespDTO updateSupplier(Integer currentUserId, Integer supplierId, UpdateSupplierReqDTO request);

    /**
     * Delete Eligible Supplier: Xóa nhà cung cấp hợp lệ (chưa phát sinh hóa đơn mua hàng)
     */
    void deleteSupplier(Integer currentUserId, Integer supplierId);
}

