package com.mycropdiary.api.controller.farmregistration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResp;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import com.mycropdiary.api.exception.GlobalExceptionHandler;
import com.mycropdiary.api.service.farmregistration.FarmRegistrationService;
import com.mycropdiary.api.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FarmRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FarmRegistrationService farmRegistrationService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private FarmRegistrationController farmRegistrationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(farmRegistrationController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/farm-registrations: Tạo đơn đăng ký với 7 trường trả về 201 CREATED")
    void createRegistration_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(10);

        FarmRegistrationCreateReq req = new FarmRegistrationCreateReq(
                "Nong Trai Hoa Sen",
                "123 Duong Tran Phu",
                "Dong Thap",
                "TP Cao Lanh",
                "Phuong 1",
                "Mo ta nong trai sen sach",
                "https://example.com/certificate.pdf"
        );

        FarmRegistrationResp resp = new FarmRegistrationResp(
                1,
                "Nong Trai Hoa Sen",
                "123 Duong Tran Phu",
                "Dong Thap",
                "TP Cao Lanh",
                "Phuong 1",
                "Mo ta nong trai sen sach",
                "https://example.com/certificate.pdf",
                FarmRegistrationStatus.PENDING,
                LocalDateTime.now(),
                null,
                null
        );

        when(farmRegistrationService.createRegistration(eq(10), any(FarmRegistrationCreateReq.class)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/farm-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationId").value(1))
                .andExpect(jsonPath("$.data.farmName").value("Nong Trai Hoa Sen"))
                .andExpect(jsonPath("$.data.addressLine").value("123 Duong Tran Phu"))
                .andExpect(jsonPath("$.data.province").value("Dong Thap"))
                .andExpect(jsonPath("$.data.district").value("TP Cao Lanh"))
                .andExpect(jsonPath("$.data.ward").value("Phuong 1"))
                .andExpect(jsonPath("$.data.description").value("Mo ta nong trai sen sach"))
                .andExpect(jsonPath("$.data.documentUrl").value("https://example.com/certificate.pdf"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/farm-registrations: Thiếu farmName hoặc addressLine -> 400 Bad Request")
    void createRegistration_InvalidRequest_Returns400() throws Exception {
        FarmRegistrationCreateReq invalidReq = new FarmRegistrationCreateReq(
                "",
                "",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/farm-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/farm-registrations: Lấy danh sách đăng ký của tôi -> 200 OK")
    void getMyRegistrations_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(10);

        FarmRegistrationSummaryResp item = new FarmRegistrationSummaryResp(
                1, "Farm A", "Dia chi", "Ha Noi", "Cau Giay", "Dich Vong",
                FarmRegistrationStatus.PENDING, LocalDateTime.now(), null, null
        );
        PageResp<FarmRegistrationSummaryResp> page = new PageResp<>(List.of(item), 0, 20, 1L, 1, true);

        when(farmRegistrationService.getMyRegistrations(eq(10), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/farm-registrations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].farmName").value("Farm A"))
                .andExpect(jsonPath("$.data.items[0].province").value("Ha Noi"))
                .andExpect(jsonPath("$.data.items[0].district").value("Cau Giay"))
                .andExpect(jsonPath("$.data.items[0].ward").value("Dich Vong"));
    }

    @Test
    @DisplayName("GET /api/v1/farm-registrations/{id}: Lấy chi tiết đơn -> 200 OK")
    void getMyRegistrationDetail_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(10);

        FarmRegistrationResp resp = new FarmRegistrationResp(
                2, "Farm B", "Dia chi B", "Bac Ninh", "Tu Son", "Dong Ngan",
                "Mo ta", "https://doc.com", FarmRegistrationStatus.PENDING,
                LocalDateTime.now(), null, null
        );

        when(farmRegistrationService.getMyRegistrationDetail(10, 2)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/farm-registrations/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationId").value(2))
                .andExpect(jsonPath("$.data.farmName").value("Farm B"))
                .andExpect(jsonPath("$.data.province").value("Bac Ninh"))
                .andExpect(jsonPath("$.data.district").value("Tu Son"))
                .andExpect(jsonPath("$.data.ward").value("Dong Ngan"));
    }

    @Test
    @DisplayName("PATCH /api/v1/farm-registrations/{id}/cancel: Hủy đơn thành công -> 200 OK")
    void cancelRegistration_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(10);

        FarmRegistrationResp resp = new FarmRegistrationResp(
                3, "Farm C", "Dia chi C", null, null, null, null, null,
                FarmRegistrationStatus.CANCELLED, LocalDateTime.now(), null, null
        );

        when(farmRegistrationService.cancelRegistration(10, 3)).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/farm-registrations/3/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
