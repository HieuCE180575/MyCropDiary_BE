package com.mycropdiary.api;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReq;
import com.mycropdiary.api.dto.block.BlockConditionResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.RecordBlockConditionReq;
import com.mycropdiary.api.dto.block.UpdateBlockReq;
import com.mycropdiary.api.dto.farm.CreateZoneReq;
import com.mycropdiary.api.dto.farm.UpdateZoneReq;
import com.mycropdiary.api.dto.farm.ZoneDetailResp;
import com.mycropdiary.api.dto.farm.ZoneResp;
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
import com.mycropdiary.api.repository.BlockConditionObservationRepository;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.BlockService;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.service.ZoneService;
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
    private ZoneService zoneService;

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

    @Autowired
    private BlockConditionObservationRepository conditionRepository;

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
        conditionRepository.deleteAll();
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

        // 2. Tạo Farm & FarmMember
        farm = farmRepository.save(new Farm("Nong Trai Xanh", "123 Duong Nong Nghiep", "Mo ta farm"));
        ownerMember = farmMemberRepository.save(new FarmMember(farm, ownerUser, "owner", "active"));
        staffMember = farmMemberRepository.save(new FarmMember(farm, staffUser, "staff", "active"));

        // 3. Tạo Zone (Khu vực / Phân khu sản xuất)
        area1 = zoneRepository.save(new Zone(farm, "KV-01", new BigDecimal("10000.00"), "Khu vuc trong lua"));
        area2 = zoneRepository.save(new Zone(farm, "KV-02", new BigDecimal("5000.00"), "Khu vuc rau sach"));
    }

    // ==========================================
    // UC-16: Quản lý phân khu (Zone)
    // ==========================================

    @Test
    @DisplayName("UC-16: Tạo, cập nhật và xem chi tiết Zone")
    void testZoneCRUD_Success() {
        CreateZoneReq createReq = new CreateZoneReq("Khu Cay An Trai", new BigDecimal("8000.00"), "Vuon sau rieng");
        ZoneResp created = zoneService.createZone(ownerUser.getId(), farm.getId(), createReq);
        assertNotNull(created.zoneId());
        assertEquals("Khu Cay An Trai", created.zoneName());

        UpdateZoneReq updateReq = new UpdateZoneReq("Khu Sau Rieng Xuat Khau", new BigDecimal("8500.00"), "m2", "Cap nhat mo ta", null);
        ZoneResp updated = zoneService.updateZone(ownerUser.getId(), created.zoneId(), updateReq);
        assertEquals("Khu Sau Rieng Xuat Khau", updated.zoneName());
        assertEquals(new BigDecimal("8500.00"), updated.areaValue());

        ZoneDetailResp detail = zoneService.getZoneById(ownerUser.getId(), created.zoneId());
        assertEquals("Khu Sau Rieng Xuat Khau", detail.zoneName());
        assertEquals(0, detail.totalBlocks());
    }

    @Test
    @DisplayName("UC-16.3, 16.4, 16.5: Tìm kiếm và lọc danh sách Zones")
    void testZoneSearch_Success() {
        List<ZoneResp> zones = zoneService.searchZones(ownerUser.getId(), farm.getId(), "KV-01", null);
        assertEquals(1, zones.size());
        assertEquals("KV-01", zones.get(0).zoneName());
    }

    // ==========================================
    // UC-14 & UC-35: Quản lý nhân viên & Phân công Zone
    // ==========================================

    @Test
    @DisplayName("UC-14: Thêm nhân sự vào trang trại thành công với role hợp lệ")
    void testAddStaff_Success() {
        AddStaffReq request = new AddStaffReq(
                outsiderUser.getEmail(),
                null,
                "staff",
                "Ky thuat vien nong nghiep"
        );

        FarmMemberResp resp = farmStaffService.addStaff(ownerUser.getId(), farm.getId(), request);
        assertNotNull(resp.id());
        assertEquals("staff", resp.farmRole());
        assertEquals(outsiderUser.getEmail(), resp.email());
    }

    @Test
    @DisplayName("UC-14: Thêm nhân sự trùng lặp -> Ném lỗi ResourceConflictException")
    void testAddStaff_AlreadyMember_ThrowsConflict() {
        AddStaffReq request = new AddStaffReq(
                staffUser.getEmail(),
                null,
                "staff",
                "Nhan vien"
        );

        assertThrows(ResourceConflictException.class, () -> 
                farmStaffService.addStaff(ownerUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-35: Phân công nhân viên vào phân khu thành công")
    void testAssignStaffToZone_Success() {
        CreateAssignmentReq request = new CreateAssignmentReq(
                staffMember.getId(),
                area1.getId(),
                Instant.now(),
                null
        );

        AssignmentResp resp = zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request);
        assertNotNull(resp.id());
        assertEquals(area1.getId(), resp.zoneId());
        assertEquals(staffMember.getId(), resp.farmMemberId());
    }

    @Test
    @DisplayName("UC-35: Phân công trùng lặp vào cùng phân khu -> Ném lỗi ResourceConflictException")
    void testAssignStaffToZone_AlreadyAssigned_ThrowsConflict() {
        CreateAssignmentReq request = new CreateAssignmentReq(
                staffMember.getId(),
                area1.getId(),
                Instant.now(),
                null
        );

        zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request);

        // Phân công lần 2
        assertThrows(ResourceConflictException.class, () -> 
                zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-35: Hủy phân công nhân viên khỏi phân khu")
    void testUnassignStaffFromZone_Success() {
        CreateAssignmentReq request = new CreateAssignmentReq(
                staffMember.getId(),
                area1.getId(),
                Instant.now(),
                null
        );
        AssignmentResp resp = zoneAssignmentService.assignStaffToZone(ownerUser.getId(), farm.getId(), request);

        // Hủy phân công
        zoneAssignmentService.unassignStaffFromZone(ownerUser.getId(), farm.getId(), resp.id());

        // Kiểm tra danh sách phân công còn active không
        List<AssignedZoneResp> assigned = zoneAssignmentService.getAssignedZonesForCurrentStaff(staffUser.getId(), farm.getId());
        assertEquals(0, assigned.size());
    }

    @Test
    @DisplayName("UC-16.1 & UC-35.1: Nhân viên xem danh sách các phân khu được phân công")
    void testGetAssignedZones_Success() {
        CreateAssignmentReq request = new CreateAssignmentReq(
                staffMember.getId(),
                area1.getId(),
                Instant.now(),
                null
        );
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

    // ==========================================
    // UC-17.4: Ghi nhận tình trạng lô đất (Record Block Condition)
    // ==========================================

    @Test
    @DisplayName("UC-17.4: Ghi nhận tình trạng lô đất và xem lịch sử tình trạng thành công")
    void testRecordBlockCondition_Success() {
        // Tạo block trước
        CreateBlockReq blockReq = new CreateBlockReq("THUA-02", "Thua Dat 2", new BigDecimal("1000.00"), null, null, null, null, null, null, null, "AVAILABLE");
        BlockResp block = blockService.createBlock(ownerUser.getId(), area1.getId(), blockReq);

        // Ghi nhận tình trạng (Record Condition)
        RecordBlockConditionReq conditionReq = new RecordBlockConditionReq(
                "Độ ẩm thấp",
                "Đất khô cằn do nắng gắt 3 ngày liên tục, cần tưới bổ sung",
                "https://images.example.com/soil_dry.jpg",
                Instant.now()
        );

        BlockConditionResp savedCondition = blockService.recordBlockCondition(ownerUser.getId(), block.blockId(), conditionReq);
        assertNotNull(savedCondition.observationId());
        assertEquals(block.blockId(), savedCondition.blockId());
        assertEquals("Độ ẩm thấp", savedCondition.conditionType());
        assertEquals("Đất khô cằn do nắng gắt 3 ngày liên tục, cần tưới bổ sung", savedCondition.description());

        // Lấy lịch sử tình trạng
        List<BlockConditionResp> history = blockService.getBlockConditions(ownerUser.getId(), block.blockId());
        assertEquals(1, history.size());
        assertEquals(savedCondition.observationId(), history.get(0).observationId());
    }
}
