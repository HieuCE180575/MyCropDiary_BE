package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.procurement.CreateInputPurchaseReq;
import com.mycropdiary.api.dto.procurement.InputPurchaseResp;
import com.mycropdiary.api.dto.procurement.LinkWarehouseReceiptReq;
import com.mycropdiary.api.dto.procurement.UpdateInputPurchaseReq;

import java.time.LocalDate;
import java.util.List;

public interface InputPurchaseService {
    /**
     * View Input Purchase List: Xem danh sách phiếu mua vật tư của trang trại
     */
    List<InputPurchaseResp> getInputPurchases(Integer currentUserId, Integer farmId);

    /**
     * View Input Purchase Details: Xem chi tiết phiếu mua vật tư
     */
    InputPurchaseResp getInputPurchaseDetails(Integer currentUserId, Integer purchaseId);

    /**
     * Search Input Purchases: Tìm kiếm phiếu mua vật tư theo điều kiện
     */
    List<InputPurchaseResp> searchInputPurchases(Integer currentUserId, Integer farmId, String keyword,
                                                Integer supplierId, LocalDate fromDate, LocalDate toDate);

    /**
     * Record Input Purchase: Ghi nhận phiếu mua vật tư mới
     */
    InputPurchaseResp recordInputPurchase(Integer currentUserId, Integer farmId, CreateInputPurchaseReq request);

    /**
     * Update Input Purchase: Chỉnh sửa phiếu mua vật tư
     */
    InputPurchaseResp updateInputPurchase(Integer currentUserId, Integer purchaseId, UpdateInputPurchaseReq request);

    /**
     * Delete Eligible Input Purchase: Xóa phiếu mua vật tư hợp lệ (chưa dùng trong nhật ký canh tác)
     */
    void deleteInputPurchase(Integer currentUserId, Integer purchaseId);

    /**
     * Link Input Purchase to Warehouse Receipt: Liên kết phiếu mua với phiếu nhập kho
     */
    InputPurchaseResp linkWarehouseReceipt(Integer currentUserId, Integer purchaseId, LinkWarehouseReceiptReq request);
}

