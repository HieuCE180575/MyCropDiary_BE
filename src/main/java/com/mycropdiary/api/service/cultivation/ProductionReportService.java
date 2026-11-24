package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.report.CropSeasonReportRespDTO;
import com.mycropdiary.api.dto.report.FarmReportRespDTO;

public interface ProductionReportService {
    FarmReportRespDTO generateFarmReport(Integer userId, Integer farmId);
    CropSeasonReportRespDTO getCropSeasonReport(Integer userId, Integer cropSeasonId);
}

