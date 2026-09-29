package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycropdiary.api.controller.cultivation.MaterialUsageController;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageRequest;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResponse;
import com.mycropdiary.api.service.cultivation.MaterialUsageService;
import com.mycropdiary.api.util.SecurityUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaterialUsageControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private MaterialUsageService materialUsageService;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MaterialUsageController materialUsageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(materialUsageController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    @DisplayName("POST /api/v1/material-usages -> 201 Created khi tạo thành công")
    void createMaterialUsage_Success() throws Exception {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                100L, 10L, null, LocalDateTime.now(), new BigDecimal("2.500"), "kg", "50g/gốc", "Rải gốc", null, "Ghi chú"
        );

        MaterialUsageResponse response = new MaterialUsageResponse(
                1L, 100L, "Bón phân", 50L, 10L, "MAT01", "Phân NPK", "FERTILIZER", null, 20L, "Chủ Nông Trại",
                request.usedAt(), request.quantity(), "kg", "50g/gốc", "Rải gốc", null, "Ghi chú"
        );

        when(materialUsageService.createMaterialUsage(eq(1L), any(CreateMaterialUsageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/material-usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.materialUsageId").value(1))
                .andExpect(jsonPath("$.data.materialName").value("Phân NPK"));
    }

    @Test
    @DisplayName("POST /api/v1/material-usages -> 400 Bad Request khi validation thất bại")
    void createMaterialUsage_ValidationFailed() throws Exception {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                null, 10L, null, null, new BigDecimal("-1.000"), "", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/material-usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/material-usages?farmingActivityId=100 -> 200 OK với PageResponse<MaterialUsageResponse>")
    void getMaterialUsagesByActivity_Success() throws Exception {
        MaterialUsageResponse response = new MaterialUsageResponse(
                1L, 100L, "Bón phân", 50L, 10L, "MAT01", "Phân NPK", "FERTILIZER", null, 20L, "Chủ Nông Trại",
                LocalDateTime.now(), new BigDecimal("2.500"), "kg", null, null, null, null
        );

        PageResponse<MaterialUsageResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of(response)));

        when(materialUsageService.getMaterialUsagesByActivity(eq(1L), eq(100L), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/material-usages")
                        .param("farmingActivityId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].materialUsageId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/material-usages/1 -> 200 OK với chi tiết vật tư")
    void getMaterialUsageById_Success() throws Exception {
        MaterialUsageResponse response = new MaterialUsageResponse(
                1L, 100L, "Bón phân", 50L, 10L, "MAT01", "Phân NPK", "FERTILIZER", null, 20L, "Chủ Nông Trại",
                LocalDateTime.now(), new BigDecimal("2.500"), "kg", null, null, null, null
        );

        when(materialUsageService.getMaterialUsageById(1L, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/material-usages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.materialUsageId").value(1));
    }
}
