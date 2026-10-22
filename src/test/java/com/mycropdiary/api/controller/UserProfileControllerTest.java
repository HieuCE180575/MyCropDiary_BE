package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycropdiary.api.dto.user.ChangePasswordReq;
import com.mycropdiary.api.dto.user.UpdateProfileReq;
import com.mycropdiary.api.dto.user.UserProfileResp;
import com.mycropdiary.api.service.UserProfileService;
import com.mycropdiary.api.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UserProfileController userProfileController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Integer userId = 10;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userProfileController).build();
    }

    @Test
    @DisplayName("GET /api/v1/user/profile -> 200 OK")
    void getProfile_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        UserProfileResp resp = new UserProfileResp(
                userId, "user@cropdiary.vn", "Nguyen Van A", "0901234567", null,
                "USER", "ACTIVE", Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );
        when(userProfileService.getProfile(userId)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"));
    }

    @Test
    @DisplayName("PUT /api/v1/user/profile -> 200 OK")
    void updateProfile_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        UpdateProfileReq req = new UpdateProfileReq("Nguyen Van B", "0987654321", null);
        UserProfileResp resp = new UserProfileResp(
                userId, "user@cropdiary.vn", "Nguyen Van B", "0987654321", null,
                "USER", "ACTIVE", Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );
        when(userProfileService.updateProfile(eq(userId), any(UpdateProfileReq.class))).thenReturn(resp);

        mockMvc.perform(put("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van B"));
    }

    @Test
    @DisplayName("POST /api/v1/user/change-password -> 200 OK")
    void changePassword_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        ChangePasswordReq req = new ChangePasswordReq("OldPass123", "NewPass123", "NewPass123");

        mockMvc.perform(post("/api/v1/user/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userProfileService).changePassword(eq(userId), any(ChangePasswordReq.class));
    }
}
