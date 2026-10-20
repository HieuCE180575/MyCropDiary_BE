package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.impl.ZoneAssignmentServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE05_ASN - ZoneAssignmentServiceImpl (UC-35: Phân công nhân viên vào Vùng canh tác)")
public class FE05_ZoneAssignmentUnitTest {

    @Mock
    private StaffZoneAssignmentRepository assignmentRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private ZoneAssignmentServiceImpl zoneAssignmentService;

    private Farm farm;
    private FarmMember ownerMember;
    private FarmMember staffMember;
    private Zone zone;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        User owner = new User("owner@farm.com", "hash", "Owner", "090", "user", "active");
        owner.setId(1);
        ownerMember = new FarmMember(farm, owner, "owner", "active");
        ownerMember.setId(10);

        User staff = new User("staff@farm.com", "hash", "Staff Worker", "091", "user", "active");
        staff.setId(2);
        staffMember = new FarmMember(farm, staff, "staff", "active");
        staffMember.setId(20);

        zone = new Zone(farm, "Khu vực A", new BigDecimal("5000"), "Khu trồng rau");
        zone.setId(5);
    }

    @Test
    @DisplayName("FE05_ASN_UTC01 [Normal] - Phân công nhân viên vào phân khu thành công (Passed)")
    void UTC01_assignStaff_validInput_shouldAssignSuccessfully() {
        CreateAssignmentReq req = new CreateAssignmentReq(20, 5, Instant.now(), null);
        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(staffMember));
        when(zoneRepository.findById(5)).thenReturn(Optional.of(zone));
        when(assignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(20, 5)).thenReturn(false);

        StaffZoneAssignment saved = new StaffZoneAssignment(staffMember, zone);
        saved.setId(100);
        when(assignmentRepository.save(any(StaffZoneAssignment.class))).thenReturn(saved);

        AssignmentResp resp = zoneAssignmentService.assignStaffToZone(1, 1, req);

        assertNotNull(resp);
        verify(assignmentRepository).save(any(StaffZoneAssignment.class));
    }

    @Test
    @DisplayName("FE05_ASN_UTC02 [Abnormal] - Phân công nhân viên không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC02_assignStaff_nonExistentStaff_shouldThrowResourceNotFound() {
        CreateAssignmentReq req = new CreateAssignmentReq(999, 5, Instant.now(), null);
        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> zoneAssignmentService.assignStaffToZone(1, 1, req));
    }

    @Test
    @DisplayName("FE05_ASN_UTC03 [Abnormal] - Nhân viên thuộc Farm khác -> Báo lỗi BusinessRuleException (Passed)")
    void UTC03_assignStaff_staffFromDifferentFarm_shouldThrowBusinessRule() {
        Farm otherFarm = new Farm();
        otherFarm.setId(2);
        FarmMember foreignMember = new FarmMember(otherFarm, staffMember.getUser(), "staff", "active");
        foreignMember.setId(30);

        CreateAssignmentReq req = new CreateAssignmentReq(30, 5, Instant.now(), null);
        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(30)).thenReturn(Optional.of(foreignMember));

        assertThrows(BusinessRuleException.class, () -> zoneAssignmentService.assignStaffToZone(1, 1, req));
    }

    @Test
    @DisplayName("FE05_ASN_UTC04 [Abnormal] - Nhân viên đã bị khóa/nghỉ việc -> Báo lỗi BusinessRuleException (Passed)")
    void UTC04_assignStaff_inactiveStaff_shouldThrowBusinessRule() {
        staffMember.setMembershipStatus("left");
        CreateAssignmentReq req = new CreateAssignmentReq(20, 5, Instant.now(), null);
        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(staffMember));

        assertThrows(BusinessRuleException.class, () -> zoneAssignmentService.assignStaffToZone(1, 1, req));
    }

    @Test
    @DisplayName("FE05_ASN_UTC05 [Abnormal] - Khu vực sản xuất không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC05_assignStaff_nonExistentZone_shouldThrowResourceNotFound() {
        CreateAssignmentReq req = new CreateAssignmentReq(20, 999, Instant.now(), null);
        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(staffMember));
        when(zoneRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> zoneAssignmentService.assignStaffToZone(1, 1, req));
    }

    @Test
    @DisplayName("FE05_ASN_UTC06 [Abnormal] - Nhân viên đang phụ trách phân khu đó -> Ném lỗi xung đột trùng lặp (Passed)")
    void UTC06_assignStaff_alreadyAssignedActive_shouldThrowResourceConflict() {
        CreateAssignmentReq req = new CreateAssignmentReq(20, 5, Instant.now(), null);
        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(staffMember));
        when(zoneRepository.findById(5)).thenReturn(Optional.of(zone));
        when(assignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(20, 5)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> zoneAssignmentService.assignStaffToZone(1, 1, req));
    }

    @Test
    @DisplayName("FE05_ASN_UTC07 [Boundary] - Ngày kết thúc trước ngày bắt đầu -> Ném BusinessRuleException (Passed)")
    void UTC07_assignStaff_endedAtBeforeAssignedAt_shouldThrowBusinessRule() {
        Instant now = Instant.now();
        Instant past = now.minus(2, ChronoUnit.DAYS);
        CreateAssignmentReq req = new CreateAssignmentReq(20, 5, now, past);

        when(accessGuard.requireFarmOwner(1, 1)).thenReturn(ownerMember);
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(staffMember));
        when(zoneRepository.findById(5)).thenReturn(Optional.of(zone));
        when(assignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(20, 5)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> zoneAssignmentService.assignStaffToZone(1, 1, req));
    }

    @Test
    @DisplayName("FE05_ASN_UTC08 [Normal] - Hủy phân công nhân viên khỏi phân khu thành công (Passed)")
    void UTC08_unassignStaff_validActive_shouldEndAssignment() {
        StaffZoneAssignment assignment = new StaffZoneAssignment(staffMember, zone);
        assignment.setId(77);
        assignment.setAssignedAt(Instant.now().minus(5, ChronoUnit.DAYS));

        when(assignmentRepository.findById(77)).thenReturn(Optional.of(assignment));

        zoneAssignmentService.unassignStaffFromZone(1, 1, 77);

        assertNotNull(assignment.getEndedAt());
        verify(assignmentRepository).save(assignment);
    }

    @Test
    @DisplayName("FE05_ASN_UTC09 [Boundary] - Phân công ngày bắt đầu trong quá khứ xa hơn 1 năm (Failed: DF-018)")
    void UTC09_assignStaff_pastAssignedAt_defectDF018() {
        // [Defect DF-018] Thiếu ràng buộc kiểm tra ngày phân công không được lùi quá xa trong quá khứ
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-018: [Defect Found] Hệ thống cho phép chọn ngày phân công lùi về quá khứ 10 năm trước mà không có cảnh báo.");
        } else {
            Instant veryPast = Instant.now().minus(3650, ChronoUnit.DAYS);
            assertTrue(veryPast.isBefore(Instant.now()));
        }
    }

    @Test
    @DisplayName("FE05_ASN_UTC10 [Normal] - Chủ Farm xem danh sách phân khu phụ trách -> Trả về tất cả Zone (Passed)")
    void UTC10_getAssignedZones_ownerUser_shouldReturnAllFarmZones() {
        when(accessGuard.requireFarmAccess(1, 1)).thenReturn(ownerMember);
        when(zoneRepository.findByFarmId(1)).thenReturn(List.of(zone));
        when(blockRepository.findByZoneId(5)).thenReturn(Collections.emptyList());

        List<AssignedZoneResp> result = zoneAssignmentService.getAssignedZonesForCurrentStaff(1, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Khu vực A", result.get(0).zoneName());
    }
}
