package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.plot.CreatePlotRequest;
import com.mycropdiary.api.dto.plot.PlotResponse;
import com.mycropdiary.api.dto.plot.UpdatePlotRequest;

import java.util.List;

public interface PlotService {
    PlotResponse createPlot(Long currentUserId, Long productionAreaId, CreatePlotRequest request);
    PlotResponse updatePlot(Long currentUserId, Long plotId, UpdatePlotRequest request);
    PlotResponse getPlotById(Long currentUserId, Long plotId);
    List<PlotResponse> searchPlots(Long currentUserId, Long farmId, Long productionAreaId, String status, String keyword);
    void deletePlot(Long currentUserId, Long plotId);
}
