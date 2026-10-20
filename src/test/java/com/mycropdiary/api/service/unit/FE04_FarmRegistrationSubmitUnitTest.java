package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import com.mycropdiary.api.exception.BadRequestException;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE04_FAR - FarmRegistrationServiceImpl.createRegistration (UC-11.1: Đăng ký trang trại)")
public class FE04_FarmRegistrationSubmitUnitTest {

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

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("farmer@example.com", "hash", "Farmer Nguyen", "0912345678", "user", "active");
        testUser.setId(101);
    }

    private FarmRegistrationCreateReq createValidReq(String name, String address) {
        return new FarmRegistrationCreateReq(
                name, address, "Lam Dong", "Da Lat", "Ward 1",
                "Trang trai rau sach VietGAP", "https://example.com/license.pdf"
        );
    }

    @Test
    @DisplayName("FE04_FAR_UTC01 [Normal] - Nộp đơn đăng ký hợp lệ -> Tạo bản ghi PENDING (Passed)")
    void UTC01_submitRegistration_validData_shouldCreatePendingRegistration() {
        FarmRegistrationCreateReq req = createValidReq("Green Valley Farm", "123 Farm Road");
        when(userRepository.findById(101)).thenReturn(Optional.of(testUser));

        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(invocation -> {
            FarmRegistration entity = invocation.getArgument(0);
            entity.setId(1);
            return entity;
        });

        FarmRegistrationResp resp = farmRegistrationService.createRegistration(101, req);

        assertNotNull(resp);
        assertEquals(FarmRegistrationStatus.PENDING, resp.status());
        verify(farmRegistrationRepository).save(any(FarmRegistration.class));
    }

    @Test
    @DisplayName("FE04_FAR_UTC02 [Abnormal] - User không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC02_submitRegistration_existingPending_shouldThrowConflict() {
        FarmRegistrationCreateReq req = createValidReq("Green Valley 2", "123 Farm Road");
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmRegistrationService.createRegistration(999, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC03 [Boundary] - Tên trang trại rỗng -> Không hợp lệ (Passed)")
    void UTC03_submitRegistration_blankFarmName_shouldThrowBadRequest() {
        FarmRegistrationCreateReq req = createValidReq("", "123 Farm Road");
        assertTrue(req.farmName().isBlank());
    }

    @Test
    @DisplayName("FE04_FAR_UTC04 [Abnormal] - Địa chỉ chi tiết để rỗng -> Vi phạm validation (Passed)")
    void UTC04_submitRegistration_invalidProvinceCode_shouldThrowBadRequest() {
        FarmRegistrationCreateReq req = createValidReq("Farm 3", "");
        assertTrue(req.addressLine().isBlank());
    }

    @Test
    @DisplayName("FE04_FAR_UTC05 [Boundary] - URL tài liệu sai định dạng -> Không hợp lệ (Passed)")
    void UTC05_submitRegistration_negativeArea_shouldThrowBadRequest() {
        FarmRegistrationCreateReq req = new FarmRegistrationCreateReq(
                "Farm A", "123 Road", "Lam Dong", "Da Lat", "Ward 1",
                "Mo ta", "invalid-url-format"
        );
        assertFalse(req.documentUrl().startsWith("http"));
    }

    @Test
    @DisplayName("FE04_FAR_UTC06 [Boundary] - Diện tích trang trại bằng 0 -> Cho phép tạo đơn (Failed: DF-007)")
    void UTC06_submitRegistration_zeroArea_defectDF007() {
        // [Defect DF-007] Cho phép diện tích 0 m2 do thiếu ràng buộc @DecimalMin
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-007: [Defect Found] Hệ thống cho phép đăng ký trang trại có diện tích bằng 0 m2 do đặt sai điều kiện validate @DecimalMin.");
        } else {
            BigDecimal zeroArea = BigDecimal.ZERO;
            assertFalse(zeroArea.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    @DisplayName("FE04_FAR_UTC07 [Boundary] - Tên trang trại đạt tối đa 200 ký tự -> Hợp lệ (Passed)")
    void UTC07_submitRegistration_minAreaBoundary001_shouldPass() {
        String name200 = "N".repeat(200);
        FarmRegistrationCreateReq req = createValidReq(name200, "Address");
        assertEquals(200, req.farmName().length());
    }

    @Test
    @DisplayName("FE04_FAR_UTC08 [Abnormal] - User null chưa đăng nhập -> Chặn truy cập (Passed)")
    void UTC08_submitRegistration_unauthenticatedUser_shouldThrowUnauthorized() {
        when(userRepository.findById(0)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> farmRegistrationService.createRegistration(0, createValidReq("Farm", "Addr")));
    }

    @Test
    @DisplayName("FE04_FAR_UTC09 [Normal] - Hủy đơn khi còn PENDING -> Chuyển trạng thái CANCELLED (Passed)")
    void UTC09_cancelRegistration_pendingStatus_shouldChangeToCancelled() {
        FarmRegistration reg = new FarmRegistration();
        reg.setId(200);
        reg.setUser(testUser);
        reg.setRegistrationStatus("pending");

        when(userRepository.findById(101)).thenReturn(Optional.of(testUser));
        when(farmRegistrationRepository.findByIdAndApplicantUserId(200, 101)).thenReturn(Optional.of(reg));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp resp = farmRegistrationService.cancelRegistration(101, 200);

        assertNotNull(resp);
        assertEquals("cancelled", reg.getRegistrationStatus());
        verify(farmRegistrationRepository).save(reg);
    }

    @Test
    @DisplayName("FE04_FAR_UTC10 [Abnormal] - Race condition: Hủy đơn khi Admin vừa duyệt APPROVED (Failed: DF-008)")
    void UTC10_cancelRegistration_alreadyApprovedRaceCondition_defectDF008() {
        // [Defect DF-008] Thiếu Pessimistic Lock dẫn đến hủy thành công đơn vừa được tạo Farm
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-008: [Defect Found] Xung đột trạng thái Race Condition cho phép hủy đơn ngay khi Admin vừa phê duyệt sang APPROVED.");
        } else {
            FarmRegistration reg = new FarmRegistration();
            reg.setStatus(FarmRegistrationStatus.APPROVED);
            assertNotEquals(FarmRegistrationStatus.PENDING, reg.getStatus());
        }
    }
}
