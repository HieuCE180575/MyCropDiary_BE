package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.assignment.AssignedAreaResponse;
import com.mycropdiary.api.dto.assignment.AssignmentResponse;
import com.mycropdiary.api.dto.assignment.CreateAssignmentRequest;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.StaffAreaAssignment;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.PlotRepository;
import com.mycropdiary.api.repository.ProductionAreaRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.service.AreaAssignmentService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AreaAssignmentServiceImpl implements AreaAssignmentService {
    private final StaffAreaAssignmentRepository assignmentRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final ProductionAreaRepository productionAreaRepository;
    private final PlotRepository plotRepository;
    private final FarmAccessGuard accessGuard;

    public AreaAssignmentServiceImpl(StaffAreaAssignmentRepository assignmentRepository,
                                    FarmMemberRepository farmMemberRepository,
                                    ProductionAreaRepository productionAreaRepository,
                                    PlotRepository plotRepository,
                                    FarmAccessGuard accessGuard) {
        this.assignmentRepository = assignmentRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.productionAreaRepository = productionAreaRepository;
        this.plotRepository = plotRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public AssignmentResponse assignStaffToArea(Long currentUserId, Long farmId, CreateAssignmentRequest request) {
        // Business Rule: Chỉ OWNER của farm mới có quyền phân công khu vực
        FarmMember ownerMember = accessGuard.requireFarmOwner(currentUserId, farmId);

        // Kiểm tra nhân viên cần phân công
        FarmMember staffMember = farmMemberRepository.findById(request.farmMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên trang trại ID: " + request.farmMemberId()));

        if (!staffMember.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Nhân viên không thuộc trang trại này");
        }

        if (!staffMember.isActive()) {
            throw new BusinessRuleException("Nhân viên đang ở trạng thái " + staffMember.getStatus() + ", không thể phân công khu vực");
        }

        // Kiểm tra khu vực sản xuất
        ProductionArea area = productionAreaRepository.findById(request.productionAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực sản xuất ID: " + request.productionAreaId()));

        if (!area.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Khu vực sản xuất không thuộc trang trại này");
        }

        if (!"ACTIVE".equalsIgnoreCase(area.getStatus())) {
            throw new BusinessRuleException("Khu vực sản xuất đang ngừng hoạt động (INACTIVE), không thể phân công");
        }

        // Business Rule: Kiểm tra xem nhân viên đã được phân công active vào khu vực này chưa
        boolean alreadyAssigned = assignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndIsActiveTrue(
                staffMember.getId(), area.getId());
        if (alreadyAssigned) {
            throw new ResourceConflictException("Nhân viên " + staffMember.getUser().getFullName() + 
                    " đã và đang được phân công phụ trách khu vực " + area.getAreaName());
        }

        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        if (request.endDate() != null && request.endDate().isBefore(startDate)) {
            throw new BusinessRuleException("Ngày kết thúc phân công phải sau hoặc bằng ngày bắt đầu");
        }

        StaffAreaAssignment assignment = new StaffAreaAssignment(staffMember, area, ownerMember, startDate);
        assignment.setEndDate(request.endDate());

        StaffAreaAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved);
    }

    @Override
    public void unassignStaffFromArea(Long currentUserId, Long farmId, Long assignmentId) {
        // Business Rule: Chỉ OWNER mới có quyền hủy phân công
        accessGuard.requireFarmOwner(currentUserId, farmId);

        StaffAreaAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi phân công ID: " + assignmentId));

        if (!assignment.getProductionArea().getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Phân công không thuộc trang trại này");
        }

        if (Boolean.FALSE.equals(assignment.getIsActive())) {
            return; // Đã hủy từ trước
        }

        assignment.setIsActive(false);
        assignment.setEndDate(LocalDate.now());
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedAreaResponse> getAssignedAreasForCurrentStaff(Long currentUserId, Long farmId) {
        // Business Rule (UC-14): Kiểm tra quyền thành viên farm
        FarmMember currentMember = accessGuard.requireFarmAccess(currentUserId, farmId);

        // Nếu là OWNER: Có quyền xem toàn bộ khu vực của trang trại
        if (currentMember.isOwner()) {
            List<ProductionArea> allAreas = productionAreaRepository.findByFarmIdAndStatus(farmId, "ACTIVE");
            return allAreas.stream().map(area -> {
                int plotCount = plotRepository.findByProductionAreaId(area.getId()).size();
                return new AssignedAreaResponse(
                        area.getId(),
                        farmId,
                        area.getAreaCode(),
                        area.getAreaName(),
                        area.getAreaM2(),
                        area.getDescription(),
                        area.getStatus(),
                        null,
                        null,
                        plotCount
                );
            }).toList();
        }

        // Business Rule (UC-14): Với STAFF -> TRUY VẤN CHỈ TRẢ KHU VỰC ĐƯỢC GIAO (chặn truy cập trái quyền)
        List<StaffAreaAssignment> activeAssignments = assignmentRepository.findActiveAssignmentsByMemberId(currentMember.getId());
        return activeAssignments.stream().map(assignment -> {
            ProductionArea area = assignment.getProductionArea();
            int plotCount = plotRepository.findByProductionAreaId(area.getId()).size();
            return new AssignedAreaResponse(
                    area.getId(),
                    farmId,
                    area.getAreaCode(),
                    area.getAreaName(),
                    area.getAreaM2(),
                    area.getDescription(),
                    area.getStatus(),
                    assignment.getId(),
                    assignment.getStartDate(),
                    plotCount
            );
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAreaAssignments(Long currentUserId, Long farmId, Long productionAreaId) {
        // Business Rule: Chỉ OWNER của farm mới có quyền xem toàn bộ danh sách phân công
        accessGuard.requireFarmOwner(currentUserId, farmId);

        List<StaffAreaAssignment> assignments = assignmentRepository.searchAssignments(farmId, productionAreaId, true);
        return assignments.stream().map(this::toAssignmentResponse).toList();
    }

    private AssignmentResponse toAssignmentResponse(StaffAreaAssignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getFarmMember().getId(),
                a.getFarmMember().getUser().getId(),
                a.getFarmMember().getUser().getFullName(),
                a.getFarmMember().getUser().getEmail(),
                a.getFarmMember().getJobTitle(),
                a.getProductionArea().getId(),
                a.getProductionArea().getAreaCode(),
                a.getProductionArea().getAreaName(),
                a.getAssignedByMember() != null ? a.getAssignedByMember().getId() : null,
                a.getAssignedByMember() != null ? a.getAssignedByMember().getUser().getFullName() : null,
                a.getStartDate(),
                a.getEndDate(),
                a.getIsActive()
        );
    }
}
