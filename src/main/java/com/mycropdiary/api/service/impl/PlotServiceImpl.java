package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.plot.CreatePlotRequest;
import com.mycropdiary.api.dto.plot.PlotResponse;
import com.mycropdiary.api.dto.plot.UpdatePlotRequest;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Plot;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.StaffAreaAssignment;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.PlotRepository;
import com.mycropdiary.api.repository.ProductionAreaRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.service.PlotService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlotServiceImpl implements PlotService {
    private final PlotRepository plotRepository;
    private final ProductionAreaRepository productionAreaRepository;
    private final StaffAreaAssignmentRepository assignmentRepository;
    private final FarmAccessGuard accessGuard;

    public PlotServiceImpl(PlotRepository plotRepository,
                           ProductionAreaRepository productionAreaRepository,
                           StaffAreaAssignmentRepository assignmentRepository,
                           FarmAccessGuard accessGuard) {
        this.plotRepository = plotRepository;
        this.productionAreaRepository = productionAreaRepository;
        this.assignmentRepository = assignmentRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public PlotResponse createPlot(Long currentUserId, Long productionAreaId, CreatePlotRequest request) {
        // Business Rule: Kiểm tra quyền truy cập khu vực (chỉ Owner hoặc Staff được phân công khu vực này)
        ProductionArea area = accessGuard.requireProductionAreaAccess(currentUserId, productionAreaId);

        // Business Rule: Mã thửa đất phải là duy nhất trong cùng khu vực sản xuất
        if (plotRepository.existsByProductionAreaIdAndPlotCodeIgnoreCase(productionAreaId, request.plotCode().trim())) {
            throw new ResourceConflictException("Mã thửa đất '" + request.plotCode().trim() + "' đã tồn tại trong khu vực sản xuất này");
        }

        // Business Rule: Kiểm tra tọa độ nếu có
        validateCoordinates(request.latitude(), request.longitude());

        // Business Rule: Tổng diện tích các thửa không vượt quá diện tích khu vực sản xuất (nếu có giới hạn)
        validateAreaCapacity(area, request.areaM2(), null);

        String status = (request.status() != null && !request.status().isBlank()) ? request.status().toUpperCase() : "AVAILABLE";

        Plot plot = new Plot(
                area,
                request.plotCode().trim().toUpperCase(),
                request.plotName().trim(),
                request.areaM2(),
                request.latitude(),
                request.longitude(),
                request.boundaryGeoJson(),
                status
        );

        Plot saved = plotRepository.save(plot);
        return toPlotResponse(saved);
    }

    @Override
    public PlotResponse updatePlot(Long currentUserId, Long plotId, UpdatePlotRequest request) {
        // Business Rule: Kiểm tra quyền truy cập thửa đất
        Plot plot = accessGuard.requirePlotAccess(currentUserId, plotId);
        ProductionArea area = plot.getProductionArea();

        // Business Rule: Kiểm tra trùng mã thửa đất (loại trừ chính nó)
        if (plotRepository.existsByProductionAreaIdAndPlotCodeIgnoreCaseAndIdNot(area.getId(), request.plotCode().trim(), plotId)) {
            throw new ResourceConflictException("Mã thửa đất '" + request.plotCode().trim() + "' đã tồn tại trong khu vực sản xuất này");
        }

        // Business Rule: Kiểm tra tọa độ
        validateCoordinates(request.latitude(), request.longitude());

        // Business Rule: Kiểm tra diện tích khu vực
        validateAreaCapacity(area, request.areaM2(), plotId);

        plot.setPlotCode(request.plotCode().trim().toUpperCase());
        plot.setPlotName(request.plotName().trim());
        plot.setAreaM2(request.areaM2());
        plot.setLatitude(request.latitude());
        plot.setLongitude(request.longitude());
        plot.setBoundaryGeoJson(request.boundaryGeoJson());
        if (request.status() != null && !request.status().isBlank()) {
            plot.setStatus(request.status().toUpperCase());
        }

        Plot saved = plotRepository.save(plot);
        return toPlotResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PlotResponse getPlotById(Long currentUserId, Long plotId) {
        // Business Rule: Kiểm tra quyền truy cập thửa đất
        Plot plot = accessGuard.requirePlotAccess(currentUserId, plotId);
        return toPlotResponse(plot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlotResponse> searchPlots(Long currentUserId, Long farmId, Long productionAreaId, String status, String keyword) {
        // Business Rule: Kiểm tra quyền truy cập vào trang trại
        FarmMember currentMember = accessGuard.requireFarmAccess(currentUserId, farmId);

        // Chặn truy cập trái quyền đối với Staff:
        if (!currentMember.isOwner()) {
            // Lấy danh sách các khu vực mà nhân viên này được phân công active
            List<StaffAreaAssignment> staffAssignments = assignmentRepository.findActiveAssignmentsByMemberId(currentMember.getId());
            Set<Long> allowedAreaIds = staffAssignments.stream()
                    .map(a -> a.getProductionArea().getId())
                    .collect(Collectors.toSet());

            if (productionAreaId != null) {
                if (!allowedAreaIds.contains(productionAreaId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Nhân viên không có quyền truy cập thửa đất thuộc khu vực này");
                }
            } else {
                // Nếu không truyền productionAreaId, chỉ trả về các thửa thuộc các khu vực được giao
                if (allowedAreaIds.isEmpty()) {
                    return List.of();
                }
            }
        }

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String trimmedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        List<Plot> plots = plotRepository.searchPlots(farmId, productionAreaId, trimmedStatus, trimmedKeyword);

        // Lọc lại một lần nữa nếu là Staff và không chọn cụ thể 1 area
        if (!currentMember.isOwner() && productionAreaId == null) {
            List<StaffAreaAssignment> staffAssignments = assignmentRepository.findActiveAssignmentsByMemberId(currentMember.getId());
            Set<Long> allowedAreaIds = staffAssignments.stream()
                    .map(a -> a.getProductionArea().getId())
                    .collect(Collectors.toSet());
            plots = plots.stream().filter(p -> allowedAreaIds.contains(p.getProductionArea().getId())).toList();
        }

        return plots.stream().map(this::toPlotResponse).toList();
    }

    @Override
    public void deletePlot(Long currentUserId, Long plotId) {
        // Business Rule: Kiểm tra quyền truy cập thửa đất
        Plot plot = accessGuard.requirePlotAccess(currentUserId, plotId);
        // Soft delete bằng cách đưa trạng thái về INACTIVE
        plot.setStatus("INACTIVE");
        plotRepository.save(plot);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            throw new BusinessRuleException("Tọa độ phải bao gồm đầy đủ cả Vĩ độ (Latitude) và Kinh độ (Longitude)");
        }
    }

    private void validateAreaCapacity(ProductionArea area, BigDecimal newPlotArea, Long excludePlotId) {
        if (area.getAreaM2() != null && area.getAreaM2().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotal = plotRepository.sumActiveAreaM2ByProductionAreaId(area.getId(), excludePlotId);
            BigDecimal totalWithNew = currentTotal.add(newPlotArea);
            if (totalWithNew.compareTo(area.getAreaM2()) > 0) {
                throw new BusinessRuleException("Tổng diện tích các thửa đất (" + totalWithNew + " m2) vượt quá diện tích của khu vực sản xuất (" + area.getAreaM2() + " m2)");
            }
        }
    }

    private PlotResponse toPlotResponse(Plot p) {
        return new PlotResponse(
                p.getId(),
                p.getProductionArea().getId(),
                p.getProductionArea().getAreaCode(),
                p.getProductionArea().getAreaName(),
                p.getProductionArea().getFarm().getId(),
                p.getPlotCode(),
                p.getPlotName(),
                p.getAreaM2(),
                p.getLatitude(),
                p.getLongitude(),
                p.getBoundaryGeoJson(),
                p.getStatus()
        );
    }
}
