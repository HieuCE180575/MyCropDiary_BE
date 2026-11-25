package com.mycropdiary.api.service.compliance;

import com.mycropdiary.api.dto.compliance.HazardousWasteReqDTO;
import com.mycropdiary.api.dto.compliance.HazardousWasteRespDTO;
import java.util.List;

/**
 * UC-47: Manage Waste Records (Mục 4.8 VietGAP, BR-98 - Dùng chung Staff + Owner).
 */
public interface HazardousWasteService {
    List<HazardousWasteRespDTO> getWasteLogs(Integer currentUserId, Integer farmId);
    HazardousWasteRespDTO createWasteLog(Integer currentUserId, Integer farmId, HazardousWasteReqDTO request);
    HazardousWasteRespDTO updateWasteLog(Integer currentUserId, Integer wasteLogId, HazardousWasteReqDTO request);
    void deleteWasteLog(Integer currentUserId, Integer wasteLogId);
}
