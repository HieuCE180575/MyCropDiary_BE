package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.controller.AdminAuditLogController;
import com.mycropdiary.api.controller.AdminFarmDirectoryController;
import com.mycropdiary.api.dto.admin.ChecklistRuleRespDTO;
import com.mycropdiary.api.dto.admin.CreateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.admin.UpdateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.staff.CreateStaffReqDTO;
import com.mycropdiary.api.dto.staff.FarmMemberRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.ChecklistRuleMapper;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.NotificationService;
import com.mycropdiary.api.service.admin.impl.ChecklistRuleServiceImpl;
import com.mycropdiary.api.service.impl.FarmStaffServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE13_AUD - Admin Farm Creation, Audit Logs, Role Normalization & Master Checklist Rule Unit Tests")
public class FE13_AdminFarmCreateAuditUnitTest {

    private static final String VALID_1HA_POLYGON =
            "[{\"lat\":10.0,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78091},{\"lat\":10.0,\"lng\":105.78091}]";

    @Mock private FarmRepository farmRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private FarmMemberRepository farmMemberRepository;
    @Mock private StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    @Mock private CropSeasonRepository cropSeasonRepository;
    @Mock private CropCategoryRepository cropCategoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private FarmRegistrationRepository registrationRepository;
    @Mock private ChecklistRuleRepository checklistRuleRepository;
    @Mock private FarmAccessGuard farmAccessGuard;
    @Mock private NotificationService notificationService;

    private AdminFarmDirectoryController adminFarmController;
    private AdminAuditLogController adminAuditLogController;
    private FarmStaffServiceImpl farmStaffService;
    private ChecklistRuleServiceImpl checklistRuleService;

    private User basicUser;
    private User staffCandidate;
    private User adminUser;
    private Farm sampleFarm;

    @BeforeEach
    void setUp() {
        adminFarmController = new AdminFarmDirectoryController(
                farmRepository, zoneRepository, blockRepository, farmMemberRepository,
                staffZoneAssignmentRepository, cropSeasonRepository, cropCategoryRepository
        );
        ReflectionTestUtils.setField(adminFarmController, "userRepository", userRepository);

        adminAuditLogController = new AdminAuditLogController(
                registrationRepository, farmRepository, userRepository, checklistRuleRepository
        );

        farmStaffService = new FarmStaffServiceImpl(
                farmMemberRepository, farmRepository, userRepository, staffZoneAssignmentRepository, farmAccessGuard
        );
        ReflectionTestUtils.setField(farmStaffService, "notificationService", notificationService);

        checklistRuleService = new ChecklistRuleServiceImpl(
                checklistRuleRepository, farmRepository, new ChecklistRuleMapper()
        );

        basicUser = new User("owner.new@farm.vn", "hash", "Nguyễn Văn Chủ", "0901111111", "user", "active");
        basicUser.setId(20);
        basicUser.setCreatedAt(Instant.parse("2026-11-25T10:00:00Z"));

        staffCandidate = new User("staff.new@farm.vn", "hash", "Trần Thị Nhân Viên", "0902222222", "user", "active");
        staffCandidate.setId(21);
        staffCandidate.setCreatedAt(Instant.parse("2026-11-25T11:00:00Z"));

        adminUser = new User("admin@mycropdiary.vn", "hash", "Quản Trị Viên", "0988888888", "admin", "active");
        adminUser.setId(1);

        sampleFarm = new Farm();
        sampleFarm.setId(50);
        sampleFarm.setFarmName("Nông trại ĐBSCL");
        sampleFarm.setAddress("Cần Thơ");
        sampleFarm.setFarmStatus("active");
        sampleFarm.setCreatedBy(basicUser);
        sampleFarm.setCreatedAt(Instant.parse("2026-11-25T12:00:00Z"));
    }

    @Test
    @DisplayName("FE13_AUD_UTC01 [Normal] - FarmMember.normalizeMemberRole chuẩn hóa staff/technician/worker -> member và giữ nguyên owner/admin/manager")
    void UTC01_normalizeMemberRole_mapsStandardAndAliasRoles() {
        assertEquals("owner", FarmMember.normalizeMemberRole("OWNER"));
        assertEquals("admin", FarmMember.normalizeMemberRole(" Admin "));
        assertEquals("manager", FarmMember.normalizeMemberRole("manager"));
        assertEquals("member", FarmMember.normalizeMemberRole("staff"));
        assertEquals("member", FarmMember.normalizeMemberRole("technician"));
        assertEquals("member", FarmMember.normalizeMemberRole("worker"));
    }

    @Test
    @DisplayName("FE13_AUD_UTC02 [Boundary] - FarmMember.normalizeMemberRole trả về member khi null, rỗng hoặc vai trò lạ")
    void UTC02_normalizeMemberRole_nullBlankOrUnknownDefaultsToMember() {
        assertEquals("member", FarmMember.normalizeMemberRole(null));
        assertEquals("member", FarmMember.normalizeMemberRole("   "));
        assertEquals("member", FarmMember.normalizeMemberRole("super_guest"));
    }

    @Test
    @DisplayName("FE13_AUD_UTC03 [Normal] - FarmStaffServiceImpl.addStaff gửi lời mời (invited) & thông báo, chưa nâng systemRole cho đến khi xác nhận")
    void UTC03_addStaff_setsInvitedStatusAndNotifiesCandidate() {
        when(farmRepository.findById(50)).thenReturn(Optional.of(sampleFarm));
        when(userRepository.findByEmailIgnoreCase("staff.new@farm.vn")).thenReturn(Optional.of(staffCandidate));
        when(farmMemberRepository.findByFarmIdAndUserId(50, 21)).thenReturn(Optional.empty());
        when(farmMemberRepository.save(any(FarmMember.class))).thenAnswer(i -> i.getArgument(0));

        FarmMemberRespDTO resp = farmStaffService.addStaff(1, 50,
                new CreateStaffReqDTO("staff.new@farm.vn", null, "staff", "Kỹ sư hiện trường"));

        assertNotNull(resp);
        assertEquals("member", resp.farmRole());
        assertEquals("invited", resp.status());
        assertEquals("user", staffCandidate.getSystemRole());
        verify(userRepository, never()).save(staffCandidate);
        verify(notificationService).createNotification(
                eq(21), contains("Lời mời"), contains("Nông trại ĐBSCL"), eq("FARM_INVITATION"), eq("/farms"));
    }

    @Test
    @DisplayName("FE13_AUD_UTC11 [Normal] - Người được mời chấp nhận lời mời (approve) -> Chuyển trạng thái active & nâng systemRole user -> staff")
    void UTC11_respondInvitation_approveActivatesMembershipAndUpgradesRole() {
        FarmMember invited = new FarmMember(sampleFarm, staffCandidate, "member", "invited");
        invited.setId(90);
        when(farmMemberRepository.findById(90)).thenReturn(Optional.of(invited));
        when(farmMemberRepository.save(any(FarmMember.class))).thenAnswer(i -> i.getArgument(0));

        farmStaffService.respondInvitation(21, 90, "approve");

        assertEquals("active", invited.getMembershipStatus());
        assertEquals("staff", staffCandidate.getSystemRole());
        verify(userRepository).save(staffCandidate);
    }

    @Test
    @DisplayName("FE13_AUD_UTC12 [Normal] - Người được mời từ chối lời mời (reject) -> Chuyển trạng thái left & giữ nguyên systemRole")
    void UTC12_respondInvitation_rejectMarksLeftWithoutRoleUpgrade() {
        FarmMember invited = new FarmMember(sampleFarm, staffCandidate, "member", "invited");
        invited.setId(91);
        when(farmMemberRepository.findById(91)).thenReturn(Optional.of(invited));
        when(farmMemberRepository.save(any(FarmMember.class))).thenAnswer(i -> i.getArgument(0));

        farmStaffService.respondInvitation(21, 91, "reject");

        assertEquals("left", invited.getMembershipStatus());
        assertEquals("user", staffCandidate.getSystemRole());
        verify(userRepository, never()).save(staffCandidate);
    }

    @Test
    @DisplayName("FE13_AUD_UTC04 [Normal] - Admin tạo trang trại đầy đủ kèm tọa độ, ranh giới, nâng vai trò Owner & thêm Staff không trùng lặp")
    void UTC04_adminCreateFarm_assignsOwnerAndStaffAndUpgradesRoles() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("farmName", "Trang trại Hậu Giang");
        body.put("address", "Ấp 2");
        body.put("ward", "Xã Vị Tân");
        body.put("district", "Vị Thanh");
        body.put("province", "Hậu Giang");
        body.put("latitude", "10.0005");
        body.put("longitude", "105.7805");
        body.put("boundaryCoordinates", VALID_1HA_POLYGON);
        body.put("ownerUserId", 20);
        body.put("staffUserIds", List.of(20, 21)); // 20 trùng Owner -> chỉ thêm 21 làm Staff

        when(userRepository.findById(20)).thenReturn(Optional.of(basicUser));
        when(userRepository.findById(21)).thenReturn(Optional.of(staffCandidate));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> {
            Farm f = i.getArgument(0);
            f.setId(55);
            return f;
        });
        when(farmMemberRepository.save(any(FarmMember.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<ApiResp<Map<String, Object>>> resp = adminFarmController.createFarm(body);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals("owner", basicUser.getSystemRole());
        assertEquals("staff", staffCandidate.getSystemRole());
        verify(farmMemberRepository, times(2)).save(any(FarmMember.class));
    }

    @Test
    @DisplayName("FE13_AUD_UTC05 [Abnormal] - Admin tạo trang trại thiếu tên hoặc thiếu địa chỉ -> Ném BadRequestException")
    void UTC05_adminCreateFarm_missingNameOrAddress_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> adminFarmController.createFarm(Map.of("address", "Cần Thơ")));
        assertThrows(BadRequestException.class,
                () -> adminFarmController.createFarm(Map.of("farmName", "Farm Thiếu Địa Chỉ")));
    }

    @Test
    @DisplayName("FE13_AUD_UTC06 [Boundary] - Admin tạo trang trại với đa giác ranh giới không hợp lệ (0 m²) -> Ném BadRequestException")
    void UTC06_adminCreateFarm_invalidPolygon_throwsBadRequest() {
        Map<String, Object> body = Map.of(
                "farmName", "Farm Lỗi Đa Giác",
                "address", "Cần Thơ",
                "boundaryCoordinates", "[[10.0,105.78],[10.001,105.78]]"
        );
        assertThrows(BadRequestException.class, () -> adminFarmController.createFarm(body));
        verify(farmRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE13_AUD_UTC07 [Normal] - AdminAuditLogController tổng hợp đầy đủ sự kiện (Registration, Farm, User locked, Master Rule) theo thứ tự mới nhất")
    void UTC07_getAuditLogs_aggregatesAndSortsNewestFirst() {
        FarmRegistration reg = new FarmRegistration(basicUser, "Farm A", "Cần Thơ", "Cần Thơ", "NK", "XK", null, null);
        reg.setId(10);
        reg.setRegistrationStatus("approved");
        reg.setReviewedBy(adminUser);
        reg.setReviewedAt(Instant.parse("2026-11-26T15:00:00Z"));

        User lockedUser = new User("locked@farm.vn", "h", "Tài Khoản Khóa", "090", "user", "locked");
        lockedUser.setId(99);
        lockedUser.setUpdatedAt(Instant.parse("2026-11-26T14:00:00Z"));

        ChecklistRule rule = new ChecklistRule();
        rule.setId(1);
        rule.setRuleCode("VG-TC-01");
        rule.setRuleName("Kiểm tra nguồn nước");
        rule.setCategory("WATER");
        rule.setIsActive(true);
        rule.setCreatedAt(Instant.parse("2026-11-26T13:00:00Z"));

        when(registrationRepository.findAll()).thenReturn(List.of(reg));
        when(farmRepository.findAll()).thenReturn(List.of(sampleFarm));
        when(userRepository.findAll()).thenReturn(List.of(lockedUser));
        when(checklistRuleRepository.findAllSystemMasterRules()).thenReturn(List.of(rule));

        ApiResp<List<Map<String, Object>>> resp = adminAuditLogController.getAuditLogs();

        assertTrue(resp.success());
        assertEquals(4, resp.data().size());
        assertEquals("reg-10", resp.data().get(0).get("id"));
        assertEquals("Duyệt hồ sơ đăng ký trang trại", resp.data().get(0).get("name"));
        assertEquals("Khóa tài khoản người dùng", resp.data().get(1).get("name"));
    }

    @Test
    @DisplayName("FE13_AUD_UTC08 [Boundary] - AdminAuditLogController giới hạn tối đa 100 bản ghi mới nhất khi hệ thống có > 100 sự kiện")
    void UTC08_getAuditLogs_capsAt100Events() {
        List<Farm> manyFarms = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            Farm f = new Farm();
            f.setId(i);
            f.setFarmName("Farm " + i);
            f.setFarmStatus("active");
            f.setUpdatedAt(Instant.parse("2026-11-26T00:00:00Z").plusSeconds(i));
            manyFarms.add(f);
        }
        when(registrationRepository.findAll()).thenReturn(List.of());
        when(farmRepository.findAll()).thenReturn(manyFarms);
        when(userRepository.findAll()).thenReturn(List.of());
        when(checklistRuleRepository.findAllSystemMasterRules()).thenReturn(List.of());

        ApiResp<List<Map<String, Object>>> resp = adminAuditLogController.getAuditLogs();

        assertEquals(100, resp.data().size());
        assertEquals("farm-120", resp.data().get(0).get("id"));
    }

    @Test
    @DisplayName("FE13_AUD_UTC09 [Normal] - ChecklistRuleServiceImpl tạo tiêu chí Master (farm=null), tự sinh mã VG-TC-XX & chuẩn hóa REQUIRED -> MUST")
    void UTC09_createChecklistRule_createsMasterRuleAndNormalizesLevel() {
        CreateChecklistRuleReqDTO req = new CreateChecklistRuleReqDTO(
                null, "Ghi chép nhật ký hóa chất", "Kiểm soát PHI đầy đủ", "FERTILIZER", "REQUIRED", "vegetable", true
        );
        when(checklistRuleRepository.count()).thenReturn(9L);
        when(checklistRuleRepository.save(any(ChecklistRule.class))).thenAnswer(i -> {
            ChecklistRule r = i.getArgument(0);
            r.setId(10);
            return r;
        });

        ChecklistRuleRespDTO resp = checklistRuleService.createChecklistRule(req);

        assertNotNull(resp);
        assertEquals("VG-TC-10", resp.ruleCode());
        assertEquals("MUST", resp.ruleLevel());
        assertEquals("VEGETABLE", resp.targetCropType());
    }

    @Test
    @DisplayName("FE13_AUD_UTC10 [Abnormal] - Cập nhật quy tắc bảng kiểm VietGAP không tồn tại -> Ném ResourceNotFoundException")
    void UTC10_updateChecklistRule_notFound_throwsResourceNotFound() {
        when(checklistRuleRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> checklistRuleService.updateChecklistRule(999,
                        new UpdateChecklistRuleReqDTO("VG-99", "Rule", "Desc", "SOIL", "RECOMMENDED", "ALL", true)));
    }
}
