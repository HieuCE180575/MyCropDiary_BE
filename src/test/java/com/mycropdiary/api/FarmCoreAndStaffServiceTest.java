package com.mycropdiary.api;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReq;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.staff.AddStaffReq;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.BlockService;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.service.ZoneAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class FarmCoreAndStaffServiceTest {

    @Autowired
    private BlockService blockService;

    @Autowired
    private FarmStaffService farmStaffService;

    @Autowired
    private ZoneAssignmentService zoneAssignmentService;

    @Autowired
    private AccountTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmMemberRepository farmMemberRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private StaffZoneAssignmentRepository assignmentRepository;

    @Autowired
    private BlockRepository blockRepository;

    private User ownerUser;
    private User staffUser;
    private User outsiderUser;
    private Farm farm;
    private FarmMember ownerMember;
    private FarmMember staffMember;
    private Zone area1;
    private Zone area2;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        blockRepository.deleteAll();
        farmMemberRepository.deleteAll();
        zoneRepository.deleteAll();
        farmRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tạo users
        ownerUser = userRepository.save(new User("owner@farm.com", "hash", "Chu Trang Trai", "0901234567", "user", "active"));
        staffUser = userRepository.save(new User("staff@farm.com", "hash", "Nhan Vien A", "0907654321", "user", "active"));
        outsiderUser = userRepository.save(new User("outsider@farm.com", "hash", "Nguoi Ngoai", "0911223344", "user", "active"));

        // 2. Tạo trang trại
        farm = farmRepository.save(new Farm("Trang Trai Xanh", "123 Duong Nong Nghiep", "Mo ta"));

        // 3. Tạo farm members
        ownerMember = farmMemberRepository.save(new FarmMember(farm, ownerUser, "owner", "active"));
        staffMember = farmMemberRepository.save(new FarmMember(farm, staffUser, "member", "active"));

        // 4. Tạo 2 khu vực sản xuất (Zone)
        area1 = zoneRepository.save(new Zone(farm, "KV-01", new BigDecimal("10000.00"), "Khu vuc rau sach"));
        area2 = zoneRepository.save(new Zone(farm, "KV-02", new BigDecimal("20000.00"), "Khu vuc cay an trai"));
    }

    // ==========================================
    // UC-33: Quản lý nhân viên trang trại
    // ==========================================

    @Test
    @DisplayName("UC-33: Owner xem danh sách nhân viên thành công")
    void testGetStaffList_Success() {
        List<FarmMemberResp> members = farmStaffService.getStaffList(ownerUser.getId(), farm.getId(), null, null);
        assertEquals(2, members.size());
    }

    @Test
    @DisplayName("UC-33: Owner thêm nhân viên mới thành công")
    void testAddStaff_Success() {
        AddStaffReq request = new AddStaffReq("outsider@farm.com", null, "MEMBER", "Nhan vien thu hoach");
        FarmMemberResp response = farmStaffService.addStaff(ownerUser.getId(), farm.getId(), request);

        assertNotNull(response.id());
        assertEquals("outsider@farm.com", response.email());
        assertEquals("MEMBER", response.farmRole().toUpperCase());
    }

    @Test
    @DisplayName("UC-33: Thêm nhân viên đã active -> Ném lỗi ResourceConflictException")
    void testAddStaff_AlreadyActive_ThrowsConflict() {
        AddStaffReq request = new AddStaffReq("staff@farm.com", null, "MEMBER", "Ky thuat");
        assertThrows(ResourceConflictException.class, () -> 
                farmStaffService.addStaff(ownerUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-33: Nhân viên cố tình thêm nhân viên -> Chặn AccessDeniedException")
    void testAddStaff_ByStaff_ThrowsAccessDenied() {
        AddStaffReq request = new AddStaffReq("outsider@farm.com", null, "MEMBER", "Nhan vien");
        assertThrows(AccessDeniedException.class, () -> 
                farmStaffService.addStaff(staffUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-33: Owner ngừng hoạt động nhân viên -> Tự động đóng tất cả phân công khu vực")
    void testDeactivateStaff_ClosesAssignments() {
        // Phân công staff vào area1
        CreateAssignmentReq assignReq = new CreateAssignmentReq(staffMember.getId(), area1.getId(), Instant.now(), null);
        zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), assignReq);

        // Ngừng hoạt động staff
        farmStaffService.deactivateStaff(ownerUser.getId(), farm.getId(), staffMember.getId());

        // Kiểm tra phân công đã bị vô hiệu hóa
        boolean hasActive = assignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(staffMember.getId(), area1.getId());
        assertFalse(hasActive);
    }

    // ==========================================
    // UC-34 / UC-14: Phân công và xem khu vực được giao (Zone Assignment)
    // ==========================================

    @Test
    @DisplayName("UC-34: Owner phân công nhân viên vào khu vực thành công")
    void testAssignStaffToArea_Success() {
        CreateAssignmentReq request = new CreateAssignmentReq(staffMember.getId(), area1.getId(), Instant.now(), null);
        AssignmentResp response = zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request);

        assertNotNull(response.id());
        assertEquals("KV-01", response.zoneName());
        assertEquals(staffMember.getId(), response.farmMemberId());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("UC-34: Phân công trùng lặp nhân viên vào khu vực đang active -> Ném lỗi ResourceConflictException")
    void testAssignStaffToArea_Duplicate_ThrowsConflict() {
        CreateAssignmentReq request = new CreateAssignmentReq(staffMember.getId(), area1.getId(), Instant.now(), null);
        zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request);

        assertThrows(ResourceConflictException.class, () -> 
                zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-14: Nhân viên xem khu vực được giao -> Chỉ trả đúng khu vực được phân công")
    void testGetAssignedAreasForCurrentStaff() {
        // Gán staff vào duy nhất area1
        CreateAssignmentReq request = new CreateAssignmentReq(staffMember.getId(), area1.getId(), Instant.now(), null);
        zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request);

        // Staff gọi API xem khu vực được giao
        List<AssignedZoneResp> assignedZones = zoneAssignmentService.getAssignedZonesForCurrentStaff(staffUser.getId(), farm.getId());
        assertEquals(1, assignedZones.size());
        assertEquals("KV-01", assignedZones.get(0).zoneName());
    }

    // ==========================================
    // UC-17: Quản lý lô / thửa đất (Block)
    // ==========================================

    @Test
    @DisplayName("UC-17: Tạo thửa đất thành công khi có quyền phụ trách khu vực")
    void testCreatePlot_Success() {
        CreateBlockReq request = new CreateBlockReq(
                "THUA-01",
                "Thua Dat 1",
                new BigDecimal("1500.00"),
                null, null, null, null,
                new BigDecimal("10.762622"),
                new BigDecimal("106.660172"),
                "{\"type\":\"Polygon\"}",
                "AVAILABLE"
        );

        BlockResp response = blockService.createBlock(ownerUser.getId(), area1.getId(), request);
        assertNotNull(response.id());
        assertEquals("THUA-01", response.blockCode());
        assertEquals(new BigDecimal("1500.00"), response.areaValue());
        assertEquals("AVAILABLE", response.status());
    }

    @Test
    @DisplayName("UC-17: Tạo thửa đất trùng mã trong cùng khu vực -> Ném lỗi ResourceConflictException")
    void testCreatePlot_DuplicateCode_ThrowsConflict() {
        CreateBlockReq request1 = new CreateBlockReq("THUA-01", "Thua 1", new BigDecimal("1000.00"), null, null, null, null, null, null, null, "AVAILABLE");
        CreateBlockReq request2 = new CreateBlockReq("THUA-01", "Thua 1 trung", new BigDecimal("500.00"), null, null, null, null, null, null, null, "AVAILABLE");

        blockService.createBlock(ownerUser.getId(), area1.getId(), request1);

        assertThrows(ResourceConflictException.class, () -> 
                blockService.createBlock(ownerUser.getId(), area1.getId(), request2));
    }

    @Test
    @DisplayName("UC-17: Nhân viên chưa được phân công khu vực tạo thửa đất -> Chặn AccessDeniedException")
    void testCreatePlot_UnassignedStaff_ThrowsAccessDenied() {
        CreateBlockReq request = new CreateBlockReq("THUA-X", "Thua X", new BigDecimal("500.00"), null, null, null, null, null, null, null, "AVAILABLE");

        // staffUser chưa được gán area2 -> phải bị chặn
        assertThrows(AccessDeniedException.class, () -> 
                blockService.createBlock(staffUser.getId(), area2.getId(), request));
    }

    @Test
    @DisplayName("UC-17: Tạo thửa đất vượt quá diện tích tối đa của khu vực -> Ném lỗi BusinessRuleException")
    void testCreatePlot_ExceedsAreaCapacity_ThrowsBusinessRule() {
        // area1 diện tích 10,000 m2 -> tạo thửa 15,000 m2 phải bị chặn
        CreateBlockReq request = new CreateBlockReq("THUA-BIG", "Thua Sieu To", new BigDecimal("15000.00"), null, null, null, null, null, null, null, "AVAILABLE");

        assertThrows(BusinessRuleException.class, () -> 
                blockService.createBlock(ownerUser.getId(), area1.getId(), request));
    }
}
