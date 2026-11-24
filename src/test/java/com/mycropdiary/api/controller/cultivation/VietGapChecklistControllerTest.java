package com.mycropdiary.api.controller.cultivation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycropdiary.api.dto.vietgap.ChecklistResultItemRespDTO;
import com.mycropdiary.api.dto.vietgap.ChecklistResultRespDTO;
import com.mycropdiary.api.dto.vietgap.InitVietGapAuditRunReqDTO;
import com.mycropdiary.api.dto.vietgap.UpdateChecklistItemReqDTO;
import com.mycropdiary.api.service.cultivation.ChecklistReviewService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VietGapChecklistControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ChecklistReviewService checklistService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ChecklistReviewController controller;

    private final Integer currentUserId = 10;
    private final Integer farmId = 1;
    private final Integer runId = 100;
    private final Integer resultId = 500;

    private ChecklistResultRespDTO sampleRunResp;
    private ChecklistResultItemRespDTO sampleItemResp;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        sampleItemResp = new ChecklistResultItemRespDTO(
                resultId,
                runId,
                1,
                "VG-TH-01",
                "Thời gian cách ly thuốc BVTV (PHI)",
                "THU_HOẠCH",
                "MUST",
                1,
                "Kỹ Sư Trưởng",
                "pass",
                "Đạt chuẩn PHI >= 14 ngày",
                null,
                "FARMING_ACTIVITY",
                12,
                true,
                false,
                Instant.now()
        );

        sampleRunResp = new ChecklistResultRespDTO(
                runId,
                null,
                "Đợt tự đánh giá VietGAP toàn diện (33 tiêu chí)",
                "VIETGAP_FULL_AUDIT",
                farmId,
                1,
                "Dưa lưới vụ Xuân",
                1,
                "Lô A1",
                1,
                "Kỹ Sư Trưởng",
                "in_progress",
                Instant.now(),
                null,
                Instant.now(),
                33,
                30,
                0,
                3,
                100.0,
                0,
                List.of(sampleItemResp)
        );
    }

    @Test
    @DisplayName("POST /api/v1/farms/{farmId}/checklist-runs/init -> 201 Created")
    void testInitAuditRun_Success() throws Exception {
        InitVietGapAuditRunReqDTO req = new InitVietGapAuditRunReqDTO(1, 1, null, null, "Ghi chú");
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(checklistService.initVietGapAuditRun(eq(currentUserId), eq(farmId), any(InitVietGapAuditRunReqDTO.class)))
                .thenReturn(sampleRunResp);

        mockMvc.perform(post("/api/v1/farms/{farmId}/checklist-runs/init", farmId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value(runId))
                .andExpect(jsonPath("$.data.runStatus").value("in_progress"))
                .andExpect(jsonPath("$.data.totalItems").value(33));
    }

    @Test
    @DisplayName("PUT /api/v1/farms/{farmId}/checklist-runs/{runId}/items/{resultId} -> 200 OK")
    void testUpdateChecklistItem_Success() throws Exception {
        UpdateChecklistItemReqDTO req = new UpdateChecklistItemReqDTO(
                "pass", "Đã kiểm nghiệm đạt chuẩn", "https://image.com/test.jpg", "DOCUMENT", 1
        );
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(checklistService.updateChecklistItem(eq(currentUserId), eq(farmId), eq(runId), eq(resultId), any(UpdateChecklistItemReqDTO.class)))
                .thenReturn(sampleItemResp);

        mockMvc.perform(put("/api/v1/farms/{farmId}/checklist-runs/{runId}/items/{resultId}", farmId, runId, resultId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resultId").value(resultId))
                .andExpect(jsonPath("$.data.resultStatus").value("pass"));
    }

    @Test
    @DisplayName("POST /api/v1/farms/{farmId}/checklist-runs/{runId}/complete -> 200 OK")
    void testCompleteChecklistRun_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(checklistService.completeChecklistRun(currentUserId, farmId, runId)).thenReturn(sampleRunResp);

        mockMvc.perform(post("/api/v1/farms/{farmId}/checklist-runs/{runId}/complete", farmId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value(runId));
    }

    @Test
    @DisplayName("POST /api/v1/farms/{farmId}/checklist-runs/{runId}/auto-check -> 200 OK")
    void testAutoCheckRun_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(checklistService.autoCheckRun(currentUserId, farmId, runId)).thenReturn(sampleRunResp);

        mockMvc.perform(post("/api/v1/farms/{farmId}/checklist-runs/{runId}/auto-check", farmId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value(runId));
    }

    @Test
    @DisplayName("GET /api/v1/farms/{farmId}/checklist-runs/{runId}/export-pdf -> 200 OK with PDF bytes")
    void testExportPdf_Success() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 mock".getBytes();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(checklistService.exportAuditReportPdf(currentUserId, farmId, runId)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/farms/{farmId}/checklist-runs/{runId}/export-pdf", farmId, runId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("GET /api/v1/checklist-runs/{runId} -> 200 OK")
    void testGetChecklistResult_Success() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(checklistService.getChecklistResult(currentUserId, runId)).thenReturn(sampleRunResp);

        mockMvc.perform(get("/api/v1/checklist-runs/{runId}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value(runId));
    }
}

