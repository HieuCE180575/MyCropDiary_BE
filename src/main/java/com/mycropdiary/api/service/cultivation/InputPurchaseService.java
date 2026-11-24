package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.procurement.CreatePurchaseReqDTO;
import com.mycropdiary.api.dto.procurement.InputPurchaseRespDTO;
import com.mycropdiary.api.dto.procurement.LinkWarehouseReceiptReqDTO;
import com.mycropdiary.api.dto.procurement.UpdatePurchaseReqDTO;

import java.time.LocalDate;
import java.util.List;

public interface InputPurchaseService {
    /**
     * View Input Purchase List: Xem danh sách phiếu mua vật tư của trang trại
     */
    List<InputPurchaseRespDTO> getInputPurchases(Integer currentUserId, Integer farmId);

    /**
     * View Input Purchase Details: Xem chi tiết phiếu mua vật tư
     */
    InputPurchaseRespDTO getInputPurchaseDetails(Integer currentUserId, Integer purchaseId);

    /**
     * Search Input Purchases: Tìm kiếm phiếu mua vật tư theo điều kiện
     */
    List<InputPurchaseRespDTO> searchInputPurchases(Integer currentUserId, Integer farmId, String keyword,
                                                Integer supplierId, LocalDate fromDate, LocalDate toDate);

    /**
     * Record Input Purchase: Ghi nhận phiếu mua vật tư mới
     */
    InputPurchaseRespDTO recordInputPurchase(Integer currentUserId, Integer farmId, CreatePurchaseReqDTO request);

    /**
     * Update Input Purchase: Chỉnh sửa phiếu mua vật tư
     */
    InputPurchaseRespDTO updateInputPurchase(Integer currentUserId, Integer purchaseId, UpdatePurchaseReqDTO request);

    /**
     * Delete Eligible Input Purchase: Xóa phiếu mua vật tư hợp lệ (chưa dùng trong nhật ký canh tác)
     */
    void deleteInputPurchase(Integer currentUserId, Integer purchaseId);

    /**
     * Link Input Purchase to Warehouse Receipt: Liên kết phiếu mua với phiếu nhập kho
     */
    InputPurchaseRespDTO linkWarehouseReceipt(Integer currentUserId, Integer purchaseId, LinkWarehouseReceiptReqDTO request);
}

