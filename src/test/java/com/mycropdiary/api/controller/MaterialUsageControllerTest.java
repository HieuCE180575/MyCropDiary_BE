package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycropdiary.api.controller.cultivation.MaterialUsageController;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.dto.cultivation.UpdateMaterialUsageReq;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private final Integer userId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer materialUsageId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer farmingActivityId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer cropSeasonId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer materialId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer recordedByMemberId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(materialUsageController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    @DisplayName("POST /api/v1/material-usages -> 201 Created khi tạo thành công")
    void createMaterialUsage_Success() throws Exception {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                farmingActivityId, materialId, null, LocalDateTime.now(), new BigDecimal("2.500"), "kg", "50g/gốc", "Rải gốc", null, "Ghi chú"
        );

        MaterialUsageResp response = new MaterialUsageResp(
                materialUsageId, farmingActivityId, "Bón phân", cropSeasonId, materialId, "MAT01", "Phân NPK", "FERTILIZER", null, recordedByMemberId, "Chủ Nông Trại",
                request.usedAt(), request.quantity(), "kg", "50g/gốc", "Rải gốc", null, "Ghi chú"
        );

        when(materialUsageService.createMaterialUsage(eq(userId), any(CreateMaterialUsageReq.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/material-usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.materialUsageId").value(materialUsageId.toString()))
                .andExpect(jsonPath("$.data.materialName").value("Phân NPK"));
    }

    @Test
    @DisplayName("POST /api/v1/material-usages -> 400 Bad Request khi validation thất bại")
    void createMaterialUsage_ValidationFailed() throws Exception {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                null, materialId, null, null, new BigDecimal("-1.000"), "", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/material-usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/material-usages/{id} -> 200 OK khi cập nhật thành công")
    void updateMaterialUsage_Success() throws Exception {
        UpdateMaterialUsageReq request = new UpdateMaterialUsageReq(
                materialId, LocalDateTime.now(), new BigDecimal("3.000"), "kg", "60g/gốc", "Rải gốc", 5, "Cập nhật liều"
        );

        MaterialUsageResp response = new MaterialUsageResp(
                materialUsageId, farmingActivityId, "Bón phân", cropSeasonId, materialId, "MAT01", "Phân NPK", "FERTILIZER", null, recordedByMemberId, "Chủ Nông Trại",
                request.usedAt(), request.quantity(), "kg", "60g/gốc", "Rải gốc", 5, "Cập nhật liều"
        );

        when(materialUsageService.updateMaterialUsage(eq(userId), eq(materialUsageId), any(UpdateMaterialUsageReq.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/material-usages/" + materialUsageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.materialUsageId").value(materialUsageId.toString()))
                .andExpect(jsonPath("$.data.notes").value("Cập nhật liều"));
    }

    @Test
    @DisplayName("PUT /api/v1/material-usages/{id} -> 400 Bad Request khi số lượng âm")
    void updateMaterialUsage_InvalidQuantity_ReturnsBadRequest() throws Exception {
        UpdateMaterialUsageReq request = new UpdateMaterialUsageReq(
                null, null, new BigDecimal("-5.000"), null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/material-usages/" + materialUsageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/material-usages?farmingActivityId=... -> 200 OK với PageResp<MaterialUsageResp>")
    void getMaterialUsagesByActivity_Success() throws Exception {
        MaterialUsageResp response = new MaterialUsageResp(
                materialUsageId, farmingActivityId, "Bón phân", cropSeasonId, materialId, "MAT01", "Phân NPK", "FERTILIZER", null, recordedByMemberId, "Chủ Nông Trại",
                LocalDateTime.now(), new BigDecimal("2.500"), "kg", null, null, null, null
        );

        PageResp<MaterialUsageResp> pageResponse = PageResp.from(new PageImpl<>(List.of(response)));

        when(materialUsageService.searchAndFilterMaterialUsages(eq(userId), eq(farmingActivityId), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/material-usages")
                        .param("farmingActivityId", farmingActivityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].materialUsageId").value(materialUsageId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/material-usages với bộ lọc nâng cao -> 200 OK")
    void getMaterialUsages_WithFilters_Success() throws Exception {
        MaterialUsageResp response = new MaterialUsageResp(
                materialUsageId, farmingActivityId, "Bón phân", cropSeasonId, materialId, "MAT01", "Phân NPK", "FERTILIZER", null, recordedByMemberId, "Chủ Nông Trại",
                LocalDateTime.now(), new BigDecimal("2.500"), "kg", null, null, null, null
        );

        PageResp<MaterialUsageResp> pageResponse = PageResp.from(new PageImpl<>(List.of(response)));

        when(materialUsageService.searchAndFilterMaterialUsages(eq(userId), isNull(), eq(cropSeasonId), eq("NPK"), eq("FERTILIZER"), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/material-usages")
                        .param("cropSeasonId", cropSeasonId.toString())
                        .param("keyword", "NPK")
                        .param("materialType", "FERTILIZER")
                        .param("fromDate", "2026-10-01T00:00:00")
                        .param("toDate", "2026-10-22T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].materialUsageId").value(materialUsageId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/material-usages/{id} -> 200 OK với chi tiết vật tư")
    void getMaterialUsageById_Success() throws Exception {
        MaterialUsageResp response = new MaterialUsageResp(
                materialUsageId, farmingActivityId, "Bón phân", cropSeasonId, materialId, "MAT01", "Phân NPK", "FERTILIZER", null, recordedByMemberId, "Chủ Nông Trại",
                LocalDateTime.now(), new BigDecimal("2.500"), "kg", null, null, null, null
        );

        when(materialUsageService.getMaterialUsageById(userId, materialUsageId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/material-usages/" + materialUsageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.materialUsageId").value(materialUsageId.toString()));
    }
}
