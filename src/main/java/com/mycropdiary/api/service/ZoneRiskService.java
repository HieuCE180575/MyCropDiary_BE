package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.risk.CreateZoneRiskReq;
import com.mycropdiary.api.dto.risk.UpdateRiskControlReq;
import com.mycropdiary.api.dto.risk.ZoneRiskResp;

import java.util.List;

public interface ZoneRiskService {
    /**
     * Record Zone Production Risk: Ghi nhận rủi ro sản xuất phân khu
     */
    ZoneRiskResp recordZoneRisk(Integer currentUserId, Integer zoneId, CreateZoneRiskReq request);

    /**
     * View Zone Risk History: Xem lịch sử rủi ro của phân khu
     */
    List<ZoneRiskResp> getZoneRiskHistory(Integer currentUserId, Integer zoneId);

    /**
     * Xem lịch sử rủi ro toàn trang trại
     */
    List<ZoneRiskResp> getFarmRiskHistory(Integer currentUserId, Integer farmId);

    /**
     * View Zone Risk Details: Xem chi tiết rủi ro
     */
    ZoneRiskResp getZoneRiskDetails(Integer currentUserId, Integer riskId);

    /**
     * Update Risk Control Measures: Cập nhật biện pháp kiểm soát rủi ro
     */
    ZoneRiskResp updateRiskControlMeasures(Integer currentUserId, Integer riskId, UpdateRiskControlReq request);

    /**
     * Xóa bản ghi rủi ro (yêu cầu quyền Farm Owner hoặc System Admin)
     */
    void deleteZoneRisk(Integer currentUserId, Integer riskId);
}

