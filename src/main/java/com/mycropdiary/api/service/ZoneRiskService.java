package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.risk.CreateZoneRiskReqDTO;
import com.mycropdiary.api.dto.risk.UpdateRiskControlReqDTO;
import com.mycropdiary.api.dto.risk.ZoneRiskRespDTO;

import java.util.List;

public interface ZoneRiskService {
    /**
     * Record Zone Production Risk: Ghi nhận rủi ro sản xuất phân khu
     */
    ZoneRiskRespDTO recordZoneRisk(Integer currentUserId, Integer zoneId, CreateZoneRiskReqDTO request);

    /**
     * View Zone Risk History: Xem lịch sử rủi ro của phân khu
     */
    List<ZoneRiskRespDTO> getZoneRiskHistory(Integer currentUserId, Integer zoneId);

    /**
     * Xem lịch sử rủi ro toàn trang trại
     */
    List<ZoneRiskRespDTO> getFarmRiskHistory(Integer currentUserId, Integer farmId);

    /**
     * View Zone Risk Details: Xem chi tiết rủi ro
     */
    ZoneRiskRespDTO getZoneRiskDetails(Integer currentUserId, Integer riskId);

    /**
     * Update Risk Control Measures: Cập nhật biện pháp kiểm soát rủi ro
     */
    ZoneRiskRespDTO updateRiskControlMeasures(Integer currentUserId, Integer riskId, UpdateRiskControlReqDTO request);

    /**
     * Xóa bản ghi rủi ro (yêu cầu quyền Farm Owner hoặc System Admin)
     */
    void deleteZoneRisk(Integer currentUserId, Integer riskId);
}

