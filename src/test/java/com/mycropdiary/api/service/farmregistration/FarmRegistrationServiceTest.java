package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.HandleRegistrationReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResp;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmRegistrationServiceTest {

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
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10);
        testUser.setEmail("farmer@example.com");
        testUser.setFullName("Nguyen Van Farmer");
        testUser.setAccountStatus("active");

        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("System Admin");
        adminUser.setAccountStatus("active");
    }

    @Test
    @DisplayName("createRegistration: Lưu đầy đủ 7 trường thông tin thành công")
    void createRegistration_AllFields_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(invocation -> {
            FarmRegistration entity = invocation.getArgument(0);
            entity.setId(100);
            return entity;
        });

        FarmRegistrationCreateReq request = new FarmRegistrationCreateReq(
                "Nong Trai Xanh Da Lat",
                "123 Duong Mai Anh Dao",
                "Lam Dong",
                "TP Da Lat",
                "Phuong 8",
                "Chuyen trong rau sach VietGAP",
                "https://example.com/docs/chung-nhan-vietgap.pdf"
        );

        FarmRegistrationResp response = farmRegistrationService.createRegistration(10, request);

        assertNotNull(response);
        assertEquals(100, response.registrationId());
        assertEquals("Nong Trai Xanh Da Lat", response.farmName());
        assertEquals("123 Duong Mai Anh Dao", response.addressLine());
        assertEquals("Lam Dong", response.province());
        assertEquals("TP Da Lat", response.district());
        assertEquals("Phuong 8", response.ward());
        assertEquals("Chuyen trong rau sach VietGAP", response.description());
        assertEquals("https://example.com/docs/chung-nhan-vietgap.pdf", response.documentUrl());
        assertEquals(FarmRegistrationStatus.PENDING, response.status());

        ArgumentCaptor<FarmRegistration> captor = ArgumentCaptor.forClass(FarmRegistration.class);
        verify(farmRegistrationRepository).save(captor.capture());
        FarmRegistration captured = captor.getValue();
        assertEquals("Nong Trai Xanh Da Lat", captured.getFarmName());
        assertEquals("123 Duong Mai Anh Dao", captured.getAddress());
        assertEquals("123 Duong Mai Anh Dao", captured.getAddressLine());
        assertEquals("Lam Dong", captured.getProvince());
        assertEquals("TP Da Lat", captured.getDistrict());
        assertEquals("Phuong 8", captured.getWard());
        assertEquals("Chuyen trong rau sach VietGAP", captured.getDescription());
        assertEquals("https://example.com/docs/chung-nhan-vietgap.pdf", captured.getDocumentUrl());
        assertEquals("pending", captured.getRegistrationStatus());
        assertEquals(testUser, captured.getUser());
    }

    @Test
    @DisplayName("createRegistration: Khi cac truong tuy chon de trong hoac blank thi luu null")
    void createRegistration_OptionalFieldsBlank_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(invocation -> {
            FarmRegistration entity = invocation.getArgument(0);
            entity.setId(101);
            return entity;
        });

        FarmRegistrationCreateReq request = new FarmRegistrationCreateReq(
                "Nong Trai Moi",
                "456 Duong Hoang Dieu",
                "   ",
                "",
                null,
                "   ",
                null
        );

        FarmRegistrationResp response = farmRegistrationService.createRegistration(10, request);

        assertNotNull(response);
        assertEquals("Nong Trai Moi", response.farmName());
        assertEquals("456 Duong Hoang Dieu", response.addressLine());
        assertNull(response.province());
        assertNull(response.district());
        assertNull(response.ward());
        assertNull(response.description());
        assertNull(response.documentUrl());
    }

    @Test
    @DisplayName("createRegistration: Nguoi dung bi khoa thi nem loi ForbiddenException")
    void createRegistration_LockedUser_ThrowsForbiddenException() {
        testUser.setAccountStatus("locked");
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        FarmRegistrationCreateReq request = new FarmRegistrationCreateReq(
                "Nong Trai Moi", "456 Duong Hoang Dieu", null, null, null, null, null
        );

        assertThrows(ForbiddenException.class, () -> farmRegistrationService.createRegistration(10, request));
        verify(farmRegistrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMyRegistrations: Tra ve danh sach tom tat bao gom province, district, ward")
    void getMyRegistrations_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        FarmRegistration reg = new FarmRegistration(
                testUser, "Nong Trai A", "789 Duong ABC", "Lam Dong", "Da Lat", "Phuong 1",
                "https://doc.url", "Ghi chu"
        );
        reg.setId(201);

        Pageable pageable = PageRequest.of(0, 10);
        when(farmRegistrationRepository.findByApplicantUserId(10, pageable))
                .thenReturn(new PageImpl<>(List.of(reg), pageable, 1));

        PageResp<FarmRegistrationSummaryResp> result = farmRegistrationService.getMyRegistrations(10, pageable);

        assertNotNull(result);
        assertEquals(1, result.items().size());
        FarmRegistrationSummaryResp summary = result.items().get(0);
        assertEquals(201, summary.registrationId());
        assertEquals("Nong Trai A", summary.farmName());
        assertEquals("789 Duong ABC", summary.addressLine());
        assertEquals("Lam Dong", summary.province());
        assertEquals("Da Lat", summary.district());
        assertEquals("Phuong 1", summary.ward());
        assertEquals(FarmRegistrationStatus.PENDING, summary.status());
    }

    @Test
    @DisplayName("getMyRegistrationDetail: Tra ve day du 7 truong theo id")
    void getMyRegistrationDetail_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        FarmRegistration reg = new FarmRegistration(
                testUser, "Nong Trai Chi Tiet", "101 Duong Le Loi", "Binh Duong", "Thu Dau Mot", "Phu Cuong",
                "https://evidence.url/doc.pdf", "Mo ta vuon cay"
        );
        reg.setId(301);

        when(farmRegistrationRepository.findByIdAndApplicantUserId(301, 10))
                .thenReturn(Optional.of(reg));

        FarmRegistrationResp resp = farmRegistrationService.getMyRegistrationDetail(10, 301);

        assertNotNull(resp);
        assertEquals(301, resp.registrationId());
        assertEquals("Nong Trai Chi Tiet", resp.farmName());
        assertEquals("101 Duong Le Loi", resp.addressLine());
        assertEquals("Binh Duong", resp.province());
        assertEquals("Thu Dau Mot", resp.district());
        assertEquals("Phu Cuong", resp.ward());
        assertEquals("Mo ta vuon cay", resp.description());
        assertEquals("https://evidence.url/doc.pdf", resp.documentUrl());
    }

    @Test
    @DisplayName("cancelRegistration: Huy don PENDING thanh cong")
    void cancelRegistration_Success() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        FarmRegistration reg = new FarmRegistration(testUser, "Nong Trai Can Huy", "Dia chi");
        reg.setId(401);
        reg.setRegistrationStatus("pending");

        when(farmRegistrationRepository.findByIdAndApplicantUserId(401, 10))
                .thenReturn(Optional.of(reg));
        when(farmRegistrationRepository.save(any(FarmRegistration.class)))
                .thenAnswer(i -> i.getArgument(0));

        FarmRegistrationResp resp = farmRegistrationService.cancelRegistration(10, 401);

        assertEquals(FarmRegistrationStatus.CANCELLED, resp.status());
        assertEquals("cancelled", reg.getRegistrationStatus());
    }

    @Test
    @DisplayName("cancelRegistration: Don da duyet thi khong the huy")
    void cancelRegistration_AlreadyApproved_ThrowsBadRequest() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));

        FarmRegistration reg = new FarmRegistration(testUser, "Nong Trai A", "Dia chi");
        reg.setId(402);
        reg.setRegistrationStatus("approved");

        when(farmRegistrationRepository.findByIdAndApplicantUserId(402, 10))
                .thenReturn(Optional.of(reg));

        assertThrows(BadRequestException.class, () -> farmRegistrationService.cancelRegistration(10, 402));
    }

    @Test
    @DisplayName("handleRegistration: Admin duyet don tao Farm thanh cong")
    void handleRegistration_Approved_CreatesFarm() {
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));

        FarmRegistration reg = new FarmRegistration(
                testUser, "Farm VietGAP", "777 Hoang Hoa Tham", "Ha Noi", "Ba Dinh", "Ngoc Ha",
                "https://cert.url", "Trong dau tay"
        );
        reg.setId(501);
        reg.setRegistrationStatus("pending");

        when(farmRegistrationRepository.findById(501)).thenReturn(Optional.of(reg));
        when(farmRepository.save(any(Farm.class))).thenAnswer(i -> {
            Farm f = i.getArgument(0);
            f.setId(10);
            return f;
        });
        when(farmRegistrationRepository.save(any(FarmRegistration.class))).thenAnswer(i -> i.getArgument(0));

        HandleRegistrationReq req = new HandleRegistrationReq("APPROVED", null);
        var resp = farmRegistrationService.handleRegistration(1, 501, req);

        assertNotNull(resp);
        assertEquals("APPROVED", resp.status());
        assertEquals("Ha Noi", resp.province());
        assertEquals("Ba Dinh", resp.district());
        assertEquals("Ngoc Ha", resp.ward());
        assertEquals("https://cert.url", resp.documentUrl());
        assertEquals(adminUser.getFullName(), resp.handlerFullName());
    }
}
