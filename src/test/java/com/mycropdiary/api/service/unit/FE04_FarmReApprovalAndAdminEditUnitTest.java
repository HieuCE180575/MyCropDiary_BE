package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farm.AdminFarmRegRespDTO;
import com.mycropdiary.api.dto.farm.CreateFarmReqDTO;
import com.mycropdiary.api.dto.farm.FarmRespDTO;
import com.mycropdiary.api.dto.farm.HandleFarmRegReqDTO;
import com.mycropdiary.api.dto.farm.UpdateFarmReqDTO;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.NotificationService;
import com.mycropdiary.api.service.impl.AdminFarmRegServiceImpl;
import com.mycropdiary.api.service.impl.FarmRegistrationServiceImpl;
import com.mycropdiary.api.service.impl.FarmServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE04_REAPP - Owner Farm Edit Re-Approval, Admin Registration Edit & Polygon Area Unit Tests (UC-11, UC-14, UC-39)")
public class FE04_FarmReApprovalAndAdminEditUnitTest {

    // ~10,000 m² (~1 ha) valid square polygon near Can Tho
    private static final String VALID_1HA_POLYGON =
            "[{\"lat\":10.0,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78091},{\"lat\":10.0,\"lng\":105.78091}]";
    // Degenerate 2-point polygon -> area = 0 m²
    private static final String INVALID_2PT_POLYGON =
            "[{\"lat\":10.0,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78}]";

    @Mock private FarmRepository farmRepository;
    @Mock private FarmMemberRepository farmMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    @Mock private FarmAccessGuard farmAccessGuard;
    @Mock private NotificationService notificationService;
    @Mock private CropSeasonRepository cropSeasonRepository;
    @Mock private FarmRegistrationRepository farmRegistrationRepository;
    @Spy  private FarmMapper farmMapper;

    private FarmServiceImpl farmService;
    private AdminFarmRegServiceImpl adminFarmRegService;
    private FarmRegistrationServiceImpl farmRegistrationService;

    private User ownerUser;
    private User adminUser;
    private Farm existingFarm;

    @BeforeEach
    void setUp() {
        farmService = new FarmServiceImpl(
                farmRepository, farmMemberRepository, userRepository, zoneRepository,
                staffZoneAssignmentRepository, farmMapper, farmAccessGuard,
                notificationService, cropSeasonRepository
        );
        ReflectionTestUtils.setField(farmService, "farmRegistrationRepository", farmRegistrationRepository);

        adminFarmRegService = new AdminFarmRegServiceImpl(
                farmRegistrationRepository, farmRepository, farmMemberRepository, userRepository, notificationService
        );
        farmRegistrationService = new FarmRegistrationServiceImpl(
                farmRegistrationRepository, userRepository, farmRepository, farmMemberRepository, notificationService
        );

        ownerUser = new User("owner@farm.vn", "hash", "Chủ Trang Trại", "0901234567", "owner", "active");
        ownerUser.setId(30);

        adminUser = new User("admin@mycropdiary.vn", "hash", "Quản Trị Viên", "0988888888", "admin", "active");
        adminUser.setId(1);

        existingFarm = new Farm();
        existingFarm.setId(10);
        existingFarm.setFarmName("Nông trại Cần Thơ");
        existingFarm.setAddress("Ninh Kiều, Cần Thơ");
        existingFarm.setDescription("Chuyên canh rau VietGAP");
        existingFarm.setFarmStatus("active");
    }

    @Test
    @DisplayName("FE04_REAPP_UTC01 [Normal] - Owner chỉnh sửa trang trại -> Tự động tạo đơn duyệt lại PENDING & gửi thông báo cho Admin")
    void UTC01_ownerUpdateFarm_createsPendingReApprovalAndNotifiesAdmins() {
        UpdateFarmReqDTO req = new UpdateFarmReqDTO(
                "Nông trại Cần Thơ Mở Rộng", "Mô tả mới", "Cái Răng, Cần Thơ",
                new BigDecimal("10.0005"), new BigDecimal("105.7805"), VALID_1HA_POLYGON,
                "https://cdn.example/cover.png", "https://cdn.example/map.pdf", "active"
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(existingFarm));
        when(zoneRepository.findByFarmId(10)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(30)).thenReturn(Optional.of(ownerUser));
        when(userRepository.findAll()).thenReturn(List.of(ownerUser, adminUser));
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmRespDTO resp = farmService.updateFarm(30, 10, req);

        assertNotNull(resp);
        assertEquals("Nông trại Cần Thơ Mở Rộng", resp.farmName());
        ArgumentCaptor<FarmRegistration> regCaptor = ArgumentCaptor.forClass(FarmRegistration.class);
        verify(farmRegistrationRepository).save(regCaptor.capture());
        FarmRegistration reviewReq = regCaptor.getValue();
        assertEquals("pending", reviewReq.getRegistrationStatus());
        assertEquals(10, reviewReq.getResultingFarmId());
        assertTrue(reviewReq.getRegistrationNote().startsWith("[Cập nhật trang trại #10]"));
        assertEquals("https://cdn.example/map.pdf", reviewReq.getDocumentUrl());
        verify(notificationService).createNotification(eq(1), contains("duyệt cập nhật"), anyString(), eq("FARM_REGISTRATION"), eq("/admin/registrations"));
    }

    @Test
    @DisplayName("FE04_REAPP_UTC02 [Normal] - Admin chỉnh sửa trang trại trực tiếp -> Không sinh đơn chờ duyệt lại")
    void UTC02_adminUpdateFarm_doesNotCreatePendingReApproval() {
        UpdateFarmReqDTO req = new UpdateFarmReqDTO(
                "Nông trại Admin Cập Nhật", "Mô tả chuẩn", "Cần Thơ",
                new BigDecimal("10.0"), new BigDecimal("105.78"), null, "active"
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(existingFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmAccessGuard.getUserRoleInFarm(1, 10)).thenReturn("admin");

        FarmRespDTO resp = farmService.updateFarm(1, 10, req);

        assertNotNull(resp);
        verify(farmRegistrationRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("FE04_REAPP_UTC03 [Normal] - Admin chỉnh sửa trực tiếp thông tin hồ sơ đăng ký trang trại thành công")
    void UTC03_adminUpdateRegistration_updatesFieldsSuccessfully() {
        FarmRegistration reg = new FarmRegistration(ownerUser, "Tên Cũ", "Địa chỉ cũ", "Cần Thơ", "Ninh Kiều", "Xuân Khánh", "https://doc.pdf", "Ghi chú cũ");
        reg.setId(501);
        reg.setRegistrationStatus("pending");

        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(reg));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        AdminFarmRegRespDTO resp = adminFarmRegService.updateRegistration(501, Map.of(
                "farmName", "  Nông trại Chuẩn VietGAP  ",
                "addressLine", "  Số 88 Đường 3/2  ",
                "province", "Cần Thơ",
                "district", "Bình Thủy",
                "ward", "Long Hòa",
                "description", "Hồ sơ đã chuẩn hóa"
        ));

        assertNotNull(resp);
        assertEquals("Nông trại Chuẩn VietGAP", reg.getFarmName());
        assertEquals("Số 88 Đường 3/2", reg.getAddress());
        assertEquals("Bình Thủy", reg.getDistrict());
        assertEquals("Long Hòa", reg.getWard());
        assertEquals("Hồ sơ đã chuẩn hóa", reg.getRegistrationNote());
    }

    @Test
    @DisplayName("FE04_REAPP_UTC04 [Abnormal] - Admin chỉnh sửa hồ sơ đăng ký không tồn tại -> Ném ResourceNotFoundException")
    void UTC04_adminUpdateRegistration_notFound_throwsResourceNotFound() {
        when(farmRegistrationRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminFarmRegService.updateRegistration(9999, Map.of("farmName", "X")));
    }

    @Test
    @DisplayName("FE04_REAPP_UTC05 [Normal] - Admin phê duyệt yêu cầu cập nhật (resultingFarmId != null) -> Cập nhật Farm cũ, bóc tiền tố & không nhân đôi Owner")
    void UTC05_adminApproveUpdateRequest_updatesExistingFarmInPlaceWithoutDuplicateMember() {
        FarmRegistration updateReg = new FarmRegistration(
                ownerUser, "Nông trại Sau Duyệt", "Địa chỉ mới", "Cần Thơ", "Cái Răng", "Lê Bình",
                "https://cdn.example/new_map.pdf", "[Cập nhật trang trại #10] Mô tả sạch sau duyệt"
        );
        updateReg.setId(502);
        updateReg.setRegistrationStatus("pending");
        updateReg.setResultingFarmId(10);

        FarmMember existingOwner = new FarmMember(existingFarm, ownerUser, "owner", "active");

        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(502)).thenReturn(Optional.of(updateReg));
        when(farmRepository.findById(10)).thenReturn(Optional.of(existingFarm));
        when(farmRepository.save(existingFarm)).thenReturn(existingFarm);
        when(farmMemberRepository.findByFarmIdAndUserId(10, 30)).thenReturn(Optional.of(existingOwner));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        AdminFarmRegRespDTO resp = adminFarmRegService.handleRegistration(1, 502, new HandleFarmRegReqDTO("APPROVED", null));

        assertNotNull(resp);
        assertEquals("APPROVED", resp.status().toUpperCase());
        assertEquals("Nông trại Sau Duyệt", existingFarm.getFarmName());
        assertEquals("Mô tả sạch sau duyệt", existingFarm.getDescription());
        assertEquals("https://cdn.example/new_map.pdf", existingFarm.getFarmMapUrl());
        verify(farmMemberRepository, never()).save(any(FarmMember.class));
    }

    @Test
    @DisplayName("FE04_REAPP_UTC06 [Boundary] - Nộp đơn đăng ký trang trại với đa giác ranh giới < 3 điểm (0 m²) -> Ném BusinessRuleException")
    void UTC06_submitRegistration_invalidPolygon_throwsBusinessRule() {
        FarmRegistrationReqDTO req = new FarmRegistrationReqDTO(
                "Farm Lỗi Bản Đồ", "123 Đường A", "Cần Thơ", "Ninh Kiều", "An Khánh",
                "Mô tả", "https://cdn.example/doc.pdf",
                new BigDecimal("10.0"), new BigDecimal("105.78"), INVALID_2PT_POLYGON
        );
        when(userRepository.findById(30)).thenReturn(Optional.of(ownerUser));

        assertThrows(BusinessRuleException.class, () -> farmRegistrationService.createRegistration(30, req));
        verify(farmRegistrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE04_REAPP_UTC07 [Boundary] - Cập nhật ranh giới trang trại nhỏ hơn tổng diện tích các phân khu hiện có -> Chặn bằng BusinessRuleException")
    void UTC07_updateFarm_boundarySmallerThanExistingZones_throwsBusinessRule() {
        Zone largeZone = new Zone(existingFarm, "Phân khu Lớn", new BigDecimal("2.0"), "2 ha = 20,000 m²");
        largeZone.setAreaUnit("ha");

        UpdateFarmReqDTO req = new UpdateFarmReqDTO(
                "Nông trại Cần Thơ", "Mô tả", "Cần Thơ",
                new BigDecimal("10.0"), new BigDecimal("105.78"), VALID_1HA_POLYGON, null, "active"
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(existingFarm));
        when(zoneRepository.findByFarmId(10)).thenReturn(List.of(largeZone));

        assertThrows(BusinessRuleException.class, () -> farmService.updateFarm(30, 10, req));
        verify(farmRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE04_REAPP_UTC08 [Boundary] - Cập nhật ranh giới trang trại lớn hơn tổng diện tích phân khu hiện có -> Lưu thành công")
    void UTC08_updateFarm_boundaryCoversExistingZones_succeeds() {
        Zone smallZone = new Zone(existingFarm, "Phân khu Nhỏ", new BigDecimal("0.5"), "0.5 ha = 5,000 m²");
        smallZone.setAreaUnit("ha");

        UpdateFarmReqDTO req = new UpdateFarmReqDTO(
                "Nông trại Cần Thơ", "Mô tả", "Cần Thơ",
                new BigDecimal("10.0"), new BigDecimal("105.78"), VALID_1HA_POLYGON, null, "active"
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(existingFarm));
        when(zoneRepository.findByFarmId(10)).thenReturn(List.of(smallZone));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(30)).thenReturn(Optional.of(ownerUser));
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmRespDTO resp = farmService.updateFarm(30, 10, req);

        assertNotNull(resp);
        verify(farmRegistrationRepository).save(any(FarmRegistration.class));
    }

    @Test
    @DisplayName("FE04_REAPP_UTC09 [Abnormal] - Tạo trang trại trực tiếp với đa giác ranh giới 2 điểm -> Chặn bằng BusinessRuleException")
    void UTC09_createFarm_degeneratePolygon_throwsBusinessRule() {
        CreateFarmReqDTO req = new CreateFarmReqDTO(
                "Farm Lỗi", "Mô tả", "Cần Thơ",
                new BigDecimal("10.0"), new BigDecimal("105.78"), INVALID_2PT_POLYGON, null, null
        );

        assertThrows(BusinessRuleException.class, () -> farmService.createFarm(30, req));
        verify(farmRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE04_REAPP_UTC10 [Abnormal] - Lỗi dịch vụ thông báo Admin khi Owner sửa Farm không làm rollback đơn duyệt lại")
    void UTC10_ownerUpdateFarm_notificationFailureIsSwallowedGracefully() {
        UpdateFarmReqDTO req = new UpdateFarmReqDTO(
                "Nông trại Bền Vững", "Mô tả", "Cần Thơ",
                new BigDecimal("10.0"), new BigDecimal("105.78"), null, "active"
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(existingFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(30)).thenReturn(Optional.of(ownerUser));
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        doThrow(new RuntimeException("Notification broker offline"))
                .when(notificationService).createNotification(any(), any(), any(), any(), any());
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmRespDTO resp = farmService.updateFarm(30, 10, req);

        assertNotNull(resp);
        verify(farmRegistrationRepository).save(any(FarmRegistration.class));
    }
}
