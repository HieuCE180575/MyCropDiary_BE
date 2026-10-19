package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReq;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ZoneAssignmentServiceImpl implements ZoneAssignmentService {
    private final StaffZoneAssignmentRepository assignmentRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final ZoneRepository zoneRepository;
    private final BlockRepository blockRepository;
    private final FarmAccessGuard accessGuard;

    public ZoneAssignmentServiceImpl(StaffZoneAssignmentRepository assignmentRepository,
                                    FarmMemberRepository farmMemberRepository,
                                    ZoneRepository zoneRepository,
                                    BlockRepository blockRepository,
                                    FarmAccessGuard accessGuard) {
        this.assignmentRepository = assignmentRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.zoneRepository = zoneRepository;
        this.blockRepository = blockRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public AssignmentResp assignStaffToZone(Integer currentUserId, Integer farmId, CreateAssignmentReq request) {
        // Business Rule: Chỉ OWNER của farm mới có quyền phân công khu vực
        FarmMember ownerMember = accessGuard.requireFarmOwner(currentUserId, farmId);

        // Kiểm tra nhân viên cần phân công
        FarmMember staffMember = farmMemberRepository.findById(request.farmMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên trang trại ID: " + request.farmMemberId()));

        if (!staffMember.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Nhân viên không thuộc trang trại này");
        }

        if (!staffMember.isActive()) {
            throw new BusinessRuleException("Nhân viên đang ở trạng thái " + staffMember.getMembershipStatus() + ", không thể phân công khu vực");
        }

        // Kiểm tra khu vực sản xuất
        Integer targetZoneId = request.zoneId() != null ? request.zoneId() : request.productionAreaId();
        Zone zone = zoneRepository.findById(targetZoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực sản xuất ID: " + targetZoneId));

        if (!zone.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Khu vực sản xuất không thuộc trang trại này");
        }

        // Business Rule: Kiểm tra xem nhân viên đã được phân công active vào khu vực này chưa
        boolean alreadyAssigned = assignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(
                staffMember.getId(), zone.getId());
        if (alreadyAssigned) {
            throw new ResourceConflictException("Nhân viên " + staffMember.getUser().getFullName() +
                    " đã và đang được phân công phụ trách khu vực " + zone.getZoneName());
        }

        Instant assignedAt = request.assignedAt() != null ? request.assignedAt() : Instant.now();
        if (request.endedAt() != null && request.endedAt().isBefore(assignedAt)) {
            throw new BusinessRuleException("Ngày kết thúc phân công phải sau hoặc bằng ngày bắt đầu");
        }

        StaffZoneAssignment assignment = new StaffZoneAssignment(staffMember, zone);
        assignment.setAssignedAt(assignedAt);
        assignment.setEndedAt(request.endedAt());

        StaffZoneAssignment saved = assignmentRepository.save(assignment);
        return toAssignmentResponse(saved, ownerMember);
    }

    @Override
    public void unassignStaffFromZone(Integer currentUserId, Integer farmId, Integer assignmentId) {
        // Business Rule: Chỉ OWNER mới có quyền hủy phân công
        accessGuard.requireFarmOwner(currentUserId, farmId);

        StaffZoneAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi phân công ID: " + assignmentId));

        if (!assignment.getZone().getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Phân công không thuộc trang trại này");
        }

        if (!assignment.isActive()) {
            throw new BusinessRuleException("Phân công này đã kết thúc trước đó");
        }

        assignment.setEndedAt(Instant.now());
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedZoneResp> getAssignedZonesForCurrentStaff(Integer currentUserId, Integer farmId) {
        FarmMember member = accessGuard.requireFarmAccess(currentUserId, farmId);

        if (member != null && member.isOwner()) {
            // Owner có quyền xem tất cả các Zone trong farm
            List<Zone> zones = zoneRepository.findByFarmId(farmId);
            return zones.stream().map(zone -> {
                int count = blockRepository.findByZoneId(zone.getId()).size();
                return new AssignedZoneResp(
                        zone.getId(),
                        farmId,
                        zone.getZoneName(),
                        zone.getAreaValue(),
                        null,
                        zone.getDescription(),
                        "active",
                        null,
                        null,
                        count
                );
            }).collect(Collectors.toList());
        }

        if (member == null) {
            return List.of();
        }

        List<StaffZoneAssignment> assignments = assignmentRepository.findActiveAssignmentsByMemberId(member.getId());
        return assignments.stream().map(a -> {
            Zone zone = a.getZone();
            int count = blockRepository.findByZoneId(zone.getId()).size();
            return new AssignedZoneResp(
                    zone.getId(),
                    farmId,
                    zone.getZoneName(),
                    zone.getAreaValue(),
                    null,
                    zone.getDescription(),
                    "active",
                    a.getId(),
                    a.getAssignedAt(),
                    count
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResp> getZoneAssignments(Integer currentUserId, Integer farmId, Integer zoneId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        List<StaffZoneAssignment> list = assignmentRepository.findAssignments(farmId, zoneId, null);
        return list.stream()
                .map(a -> toAssignmentResponse(a, null))
                .collect(Collectors.toList());
    }

    private AssignmentResp toAssignmentResponse(StaffZoneAssignment a, FarmMember assigner) {
        FarmMember fm = a.getFarmMember();
        Zone zone = a.getZone();
        return new AssignmentResp(
                a.getId(),
                fm.getId(),
                fm.getUser() != null ? fm.getUser().getId() : null,
                fm.getUser() != null ? fm.getUser().getFullName() : null,
                fm.getUser() != null ? fm.getUser().getEmail() : null,
                fm.getJobTitle(),
                zone.getId(),
                zone.getZoneName(),
                assigner != null ? assigner.getId() : null,
                assigner != null && assigner.getUser() != null ? assigner.getUser().getFullName() : null,
                a.getAssignedAt(),
                a.getEndedAt(),
                a.isActive()
        );
    }
}
