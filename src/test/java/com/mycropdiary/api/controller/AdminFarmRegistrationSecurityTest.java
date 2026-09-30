package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import com.mycropdiary.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

// [AI_CHANGE] Root cause: Kiểm tra bảo mật endpoint Admin (/api/v1/admin/farm-registrations)
// [AI_CHANGE] Mechanism: Dùng MockMvc với Spring Security filter chain kiểm tra 3 cấp độ: Anonymous, USER, ADMIN
@SpringBootTest
@ActiveProfiles("local")
class AdminFarmRegistrationSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/farm-registrations không kèm token -> Trả về 401 Unauthorized")
    void getRegistrations_Anonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/farm-registrations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yêu cầu chưa được xác thực. Vui lòng cung cấp JWT token hợp lệ."));
    }

    @Test
    @DisplayName("GET /api/v1/admin/farm-registrations với token USER -> Trả về 403 Forbidden")
    void getRegistrations_UserRole_Returns403() throws Exception {
        String userToken = jwtTokenProvider.generateAccessToken(99L, "farmer@test.com", "USER");

        mockMvc.perform(get("/api/v1/admin/farm-registrations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập tài nguyên này (yêu cầu vai trò ADMIN)."));
    }

    @Test
    @DisplayName("GET /api/v1/admin/farm-registrations với token ADMIN -> Trả về 200 OK")
    void getRegistrations_AdminRole_Returns200() throws Exception {
        String adminToken = jwtTokenProvider.generateAccessToken(1L, "admin@mycropdiary.com", "ADMIN");

        mockMvc.perform(get("/api/v1/admin/farm-registrations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/farm-registrations/1/handle với token USER -> Trả về 403 Forbidden")
    void handleRegistration_UserRole_Returns403() throws Exception {
        String userToken = jwtTokenProvider.generateAccessToken(99L, "farmer@test.com", "USER");
        HandleRegistrationRequest request = new HandleRegistrationRequest("APPROVED", null);

        mockMvc.perform(put("/api/v1/admin/farm-registrations/1/handle")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
