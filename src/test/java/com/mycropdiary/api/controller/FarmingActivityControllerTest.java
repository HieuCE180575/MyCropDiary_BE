package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycropdiary.api.controller.cultivation.FarmingActivityController;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.service.cultivation.FarmingActivityService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FarmingActivityControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private FarmingActivityService farmingActivityService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private FarmingActivityController farmingActivityController;

    private final Integer currentUserId = 10;
    private final Integer activityId = 500;
    private final Integer cropSeasonId = 1000;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(farmingActivityController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/farming-activities - Tạo nhật ký thành công (201 Created)")
    void createActivity_Success() throws Exception {
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                cropSeasonId,
                "Làm đất",
                "Cày xới luống",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                "Cày sâu 20cm",
                "Đạt chuẩn",
                "Trời nắng",
                null,
                null
        );

        FarmingActivityResp resp = new FarmingActivityResp(
                activityId,
                cropSeasonId,
                "Vụ Đông Xuân",
                "Vụ Đông Xuân",
                1,
                "Farm A",
                null, null, null,
                currentUserId,
                "Nguyễn Văn A",
                req.activityType(),
                req.activityName(),
                req.startedAt(),
                req.endedAt(),
                req.description(),
                req.resultNotes(),
                req.weatherNotes(),
                null,
                List.of(),
                req.startedAt()
        );

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(farmingActivityService.createActivity(eq(currentUserId), any(CreateFarmingActivityReq.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/farming-activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.farmingActivityId").value(activityId))
                .andExpect(jsonPath("$.data.activityName").value("Cày xới luống"));
    }

    @Test
    @DisplayName("PUT /api/v1/farming-activities/{activityId} - Cập nhật nhật ký thành công (200 OK)")
    void updateActivity_Success() throws Exception {
        UpdateFarmingActivityReq req = new UpdateFarmingActivityReq(
                "Tưới tiêu",
                "Tưới nước tự động",
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(2),
                "Tưới 30 phút",
                "Đất ẩm đều",
                "Nhiệt độ 28C",
                null,
                null,
                null,
                List.of()
        );

        FarmingActivityResp resp = new FarmingActivityResp(
                activityId,
                cropSeasonId,
                "Vụ Đông Xuân",
                "Vụ Đông Xuân",
                1,
                "Farm A",
                null, null, null,
                currentUserId,
                "Nguyễn Văn A",
                req.activityType(),
                req.activityName(),
                req.startedAt(),
                req.endedAt(),
                req.description(),
                req.resultNotes(),
                req.weatherNotes(),
                null,
                List.of(),
                req.startedAt()
        );

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(farmingActivityService.updateActivity(eq(currentUserId), eq(activityId), any(UpdateFarmingActivityReq.class)))
                .thenReturn(resp);

        mockMvc.perform(put("/api/v1/farming-activities/" + activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.farmingActivityId").value(activityId))
                .andExpect(jsonPath("$.data.activityType").value("Tưới tiêu"));
    }

    @Test
    @DisplayName("GET /api/v1/farming-activities - Tìm kiếm và lọc nhật ký theo mùa vụ (200 OK)")
    void getActivitiesByCropSeason_SearchAndFilter_Success() throws Exception {
        FarmingActivitySummaryResp summaryResp = new FarmingActivitySummaryResp(
                activityId,
                cropSeasonId,
                "Bón phân",
                "Bón thúc đợt 2",
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now().minusHours(4),
                currentUserId,
                "Nguyễn Văn A",
                2,
                LocalDateTime.now().minusHours(5)
        );

        PageResp<FarmingActivitySummaryResp> pageResp = PageResp.from(new PageImpl<>(List.of(summaryResp)));

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(farmingActivityService.searchAndFilterActivities(
                eq(currentUserId), eq(cropSeasonId), eq("bón thúc"), eq("Bón phân"), any(), any(), any(Pageable.class)
        )).thenReturn(pageResp);

        mockMvc.perform(get("/api/v1/farming-activities")
                        .param("cropSeasonId", cropSeasonId.toString())
                        .param("keyword", "bón thúc")
                        .param("activityType", "Bón phân")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].activityName").value("Bón thúc đợt 2"));
    }

    @Test
    @DisplayName("GET /api/v1/farming-activities/{activityId} - Lấy chi tiết nhật ký thành công (200 OK)")
    void getActivityDetail_Success() throws Exception {
        FarmingActivityResp resp = new FarmingActivityResp(
                activityId,
                cropSeasonId,
                "Vụ Đông Xuân",
                "Vụ Đông Xuân",
                1,
                "Farm A",
                null, null, null,
                currentUserId,
                "Nguyễn Văn A",
                "Tưới tiêu",
                "Tưới nước",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                "Mô tả",
                "Ghi chú",
                "Thời tiết",
                null,
                List.of(),
                LocalDateTime.now().minusHours(2)
        );

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(farmingActivityService.getActivityDetail(currentUserId, activityId)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/farming-activities/" + activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.farmingActivityId").value(activityId));
    }
}
