package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycropdiary.api.controller.cultivation.HarvestRecordController;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReq;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResp;
import com.mycropdiary.api.service.cultivation.HarvestRecordService;
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
class HarvestRecordControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private HarvestRecordService harvestRecordService;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private HarvestRecordController harvestRecordController;

    private final Integer userId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer harvestRecordId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer cropSeasonId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer farmId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer plotId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer recordedByMemberId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(harvestRecordController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    @DisplayName("POST /api/v1/harvest-records -> 201 Created khi tạo thành công")
    void createHarvestRecord_Success() throws Exception {
        CreateHarvestRecordReq request = new CreateHarvestRecordReq(
                cropSeasonId, "LOT-001", LocalDateTime.now(), new BigDecimal("150.500"), "kg", "Loại 1", "Kho A", "TR-001", "Ghi chú"
        );

        HarvestRecordResp response = new HarvestRecordResp(
                harvestRecordId, cropSeasonId, "S2026", "Mùa Vụ Đông Xuân", farmId, "Nông trại Việt", plotId, "P01", "Thửa 1",
                recordedByMemberId, "Chủ Nông Trại", "LOT-001", request.harvestedAt(), request.quantity(), "kg", "Loại 1", "Kho A", "TR-001", "Ghi chú"
        );

        when(harvestRecordService.createHarvestRecord(eq(userId), any(CreateHarvestRecordReq.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/harvest-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.harvestRecordId").value(harvestRecordId.toString()))
                .andExpect(jsonPath("$.data.harvestLotCode").value("LOT-001"));
    }

    @Test
    @DisplayName("POST /api/v1/harvest-records -> 400 Bad Request khi validation thất bại (Quantity <= 0)")
    void createHarvestRecord_ValidationFailed() throws Exception {
        CreateHarvestRecordReq request = new CreateHarvestRecordReq(
                null, "LOT-001", null, new BigDecimal("-10.000"), "", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/harvest-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/harvest-records?cropSeasonId=... -> 200 OK với PageResp")
    void getHarvestRecordsBySeason_Success() throws Exception {
        HarvestRecordResp response = new HarvestRecordResp(
                harvestRecordId, cropSeasonId, "S2026", "Mùa Vụ Đông Xuân", farmId, "Nông trại Việt", plotId, "P01", "Thửa 1",
                recordedByMemberId, "Chủ Nông Trại", "LOT-001", LocalDateTime.now(), new BigDecimal("150.500"), "kg", "Loại 1", "Kho A", "TR-001", "Ghi chú"
        );

        PageResp<HarvestRecordResp> pageResponse = PageResp.from(new PageImpl<>(List.of(response)));

        when(harvestRecordService.getHarvestRecordsBySeason(eq(userId), eq(cropSeasonId), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/harvest-records")
                        .param("cropSeasonId", cropSeasonId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].harvestRecordId").value(harvestRecordId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/harvest-records/{id} -> 200 OK với chi tiết bản ghi")
    void getHarvestRecordById_Success() throws Exception {
        HarvestRecordResp response = new HarvestRecordResp(
                harvestRecordId, cropSeasonId, "S2026", "Mùa Vụ Đông Xuân", farmId, "Nông trại Việt", plotId, "P01", "Thửa 1",
                recordedByMemberId, "Chủ Nông Trại", "LOT-001", LocalDateTime.now(), new BigDecimal("150.500"), "kg", "Loại 1", "Kho A", "TR-001", "Ghi chú"
        );

        when(harvestRecordService.getHarvestRecordById(userId, harvestRecordId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/harvest-records/" + harvestRecordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.harvestRecordId").value(harvestRecordId.toString()));
    }
}
