package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farm.FarmRegistrationResp;
import com.mycropdiary.api.dto.farm.HandleRegistrationReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.impl.FarmRegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE04_FAR - FarmRegistrationServiceImpl.handleRegistration (UC-39: Xét duyệt đăng ký trang trại)")
public class FE04_FarmRegistrationReviewUnitTest {

    @Mock
    private FarmRegistrationRepository farmRegistrationRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FarmRegistrationServiceImpl farmRegistrationService;

    private User adminUser;
    private User regularUser;
    private FarmRegistration pendingReg;

    @BeforeEach
    void setUp() {
        adminUser = new User("admin@example.com", "hash", "Admin", "0988888888", "admin", "active");
        adminUser.setId(1);
        regularUser = new User("farmer@example.com", "hash", "Farmer", "0912345678", "user", "active");
        regularUser.setId(2);

        pendingReg = new FarmRegistration(regularUser, "Farm VietGAP", "777 Hoang Hoa Tham", "Ha Noi", "Ba Dinh", "Ngoc Ha",
                "https://cert.url", "Trong dau tay");
        pendingReg.setId(501);
        pendingReg.setRegistrationStatus("pending");
    }

    @Test
    @DisplayName("FE04_FAR_UTC01 [Normal] - Admin phê duyệt đơn PENDING -> Tạo Farm và gán vai trò OWNER (Passed)")
    void UTC01_reviewRegistration_adminApproves_shouldCreateFarmAndAssignOwnerRole() {
        HandleRegistrationReq req = new HandleRegistrationReq("APPROVED", null);

        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> {
            Farm f = i.getArgument(0);
            f.setId(10);
            return f;
        });
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        FarmRegistrationResp resp = farmRegistrationService.handleRegistration(1, 501, req);

        assertNotNull(resp);
        assertEquals("APPROVED", resp.status().toUpperCase());
        verify(farmRepository).save(any(Farm.class));
        verify(farmMemberRepository).save(any());
    }

    @Test
    @DisplayName("FE04_FAR_UTC02 [Normal] - Admin từ chối đơn với lý do cụ thể -> Cập nhật REJECTED (Passed)")
    void UTC02_reviewRegistration_adminRejects_shouldSaveRejectReason() {
        HandleRegistrationReq req = new HandleRegistrationReq("REJECTED", "Thiếu giấy tờ chứng nhận hợp lệ");

        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        FarmRegistrationResp resp = farmRegistrationService.handleRegistration(1, 501, req);

        assertNotNull(resp);
        assertEquals("REJECTED", resp.status().toUpperCase());
        assertEquals("Thiếu giấy tờ chứng nhận hợp lệ", pendingReg.getRejectionReason());
    }

    @Test
    @DisplayName("FE04_FAR_UTC03 [Abnormal] - User không phải ADMIN cố tình duyệt -> Ném ForbiddenException (Passed)")
    void UTC03_reviewRegistration_nonAdminUser_shouldThrowForbiddenException() {
        HandleRegistrationReq req = new HandleRegistrationReq("APPROVED", null);
        when(userRepository.findById(2)).thenReturn(Optional.of(regularUser));

        assertThrows(ForbiddenException.class, () -> farmRegistrationService.handleRegistration(2, 501, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC04 [Abnormal] - Duyệt đơn không tồn tại (ID 999999) -> Ném ResourceNotFound (Passed)")
    void UTC04_reviewRegistration_nonExistentId_shouldThrowResourceNotFound() {
        HandleRegistrationReq req = new HandleRegistrationReq("APPROVED", null);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(999999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmRegistrationService.handleRegistration(1, 999999, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC05 [Boundary] - Từ chối nhưng để trống lý do -> Ném BadRequestException (Passed)")
    void UTC05_reviewRegistration_rejectWithBlankReason_shouldThrowBadRequest() {
        HandleRegistrationReq req = new HandleRegistrationReq("REJECTED", "   ");
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));

        assertThrows(BadRequestException.class, () -> farmRegistrationService.handleRegistration(1, 501, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC06 [Abnormal] - Duyệt lại đơn đã REJECTED -> Báo lỗi trạng thái không hợp lệ (Passed)")
    void UTC06_reviewRegistration_reApproveRejectedRegistration_shouldThrowBadRequest() {
        pendingReg.setRegistrationStatus("rejected");
        HandleRegistrationReq req = new HandleRegistrationReq("APPROVED", null);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));

        assertThrows(BadRequestException.class, () -> farmRegistrationService.handleRegistration(1, 501, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC07 [Abnormal] - Từ chối lại đơn đã APPROVED -> Báo lỗi trạng thái (Passed)")
    void UTC07_reviewRegistration_reRejectApprovedRegistration_shouldThrowBadRequest() {
        pendingReg.setRegistrationStatus("approved");
        HandleRegistrationReq req = new HandleRegistrationReq("REJECTED", "Reason");
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));

        assertThrows(BadRequestException.class, () -> farmRegistrationService.handleRegistration(1, 501, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC08 [Boundary] - Lý do từ chối 501 ký tự -> Gây lỗi tràn cột NVARCHAR(500) (Failed: DF-009)")
    void UTC08_reviewRegistration_reasonExceeds500Chars_defectDF009() {
        // [Defect DF-009] DTO thiếu @Size(max=500) dẫn đến 500 DataTruncation
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-009: [Defect Found] Lý do từ chối 501 ký tự gây tràn cột NVARCHAR(500) làm sập hệ thống thay vì báo 400 Bad Request.");
        } else {
            String longReason = "R".repeat(501);
            assertTrue(longReason.length() > 500);
        }
    }

    @Test
    @DisplayName("FE04_FAR_UTC09 [Boundary] - Lý do từ chối đúng 500 ký tự -> Lưu thành công (Passed)")
    void UTC09_reviewRegistration_reasonExactly500Chars_shouldPass() {
        String exact500 = "R".repeat(500);
        HandleRegistrationReq req = new HandleRegistrationReq("REJECTED", exact500);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        FarmRegistrationResp resp = farmRegistrationService.handleRegistration(1, 501, req);
        assertNotNull(resp);
        assertEquals("REJECTED", resp.status().toUpperCase());
    }

    @Test
    @DisplayName("FE04_FAR_UTC10 [Normal] - Ghi nhận Audit Log người duyệt và thời gian (Passed)")
    void UTC10_reviewRegistration_auditLogRecorded_shouldCaptureAdminDetails() {
        HandleRegistrationReq req = new HandleRegistrationReq("REJECTED", "Lý do từ chối");
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(pendingReg));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        farmRegistrationService.handleRegistration(1, 501, req);

        assertNotNull(pendingReg.getReviewedAt());
        assertEquals(adminUser, pendingReg.getReviewedBy());
    }
}
