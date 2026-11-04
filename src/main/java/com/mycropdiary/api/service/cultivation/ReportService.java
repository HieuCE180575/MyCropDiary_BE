package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.report.CropSeasonReportResp;
import com.mycropdiary.api.dto.report.FarmReportResp;

public interface ReportService {
    FarmReportResp generateFarmReport(Integer userId, Integer farmId);
    CropSeasonReportResp getCropSeasonReport(Integer userId, Integer cropSeasonId);
}

