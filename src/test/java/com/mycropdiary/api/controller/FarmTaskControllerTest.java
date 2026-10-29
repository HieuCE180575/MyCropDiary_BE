package com.mycropdiary.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycropdiary.api.controller.cultivation.FarmTaskController;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateFarmTaskReq;
import com.mycropdiary.api.dto.cultivation.FarmTaskResp;
import com.mycropdiary.api.dto.cultivation.UpdateFarmTaskStatusReq;
import com.mycropdiary.api.service.cultivation.FarmTaskService;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FarmTaskControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private FarmTaskService farmTaskService;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private FarmTaskController farmTaskController;

    private final Integer userId = 10;
    private final Integer farmId = 1;
    private final Integer taskId = 100;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(farmTaskController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    @DisplayName("GET /api/v1/farms/{farmId}/tasks -> 200 OK")
    void getFarmTasks_Success() throws Exception {
        FarmTaskResp response = new FarmTaskResp(
                taskId, farmId, "Trang trại mẫu", null, null, null, null, null, null,
                "Tưới nhỏ giọt", "Ghi chú", "Tưới tiêu", "normal", "planned",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                userId, "Chủ trang trại", List.of(), Instant.now()
        );

        PageResp<FarmTaskResp> pageResponse = PageResp.from(new PageImpl<>(List.of(response)));

        when(farmTaskService.getFarmTasks(eq(userId), eq(farmId), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/farms/" + farmId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.data.items[0].taskName").value("Tưới nhỏ giọt"));
    }

    @Test
    @DisplayName("POST /api/v1/farms/{farmId}/tasks -> 201 Created")
    void createTask_Success() throws Exception {
        CreateFarmTaskReq req = new CreateFarmTaskReq(
                "Tưới nhỏ giọt", "Tưới tiêu", "normal", "planned",
                null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "Ghi chú"
        );

        FarmTaskResp response = new FarmTaskResp(
                taskId, farmId, "Trang trại mẫu", null, null, null, null, null, null,
                "Tưới nhỏ giọt", "Ghi chú", "Tưới tiêu", "normal", "planned",
                req.startsAt(), req.dueAt(),
                userId, "Chủ trang trại", List.of(), Instant.now()
        );

        when(farmTaskService.createTask(eq(userId), eq(farmId), any(CreateFarmTaskReq.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/farms/" + farmId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{taskId}/status -> 200 OK")
    void updateTaskStatus_Success() throws Exception {
        UpdateFarmTaskStatusReq req = new UpdateFarmTaskStatusReq("completed");

        FarmTaskResp response = new FarmTaskResp(
                taskId, farmId, "Trang trại mẫu", null, null, null, null, null, null,
                "Tưới nhỏ giọt", "Ghi chú", "Tưới tiêu", "normal", "completed",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                userId, "Chủ trang trại", List.of(), Instant.now()
        );

        when(farmTaskService.updateTaskStatus(eq(userId), eq(taskId), any(UpdateFarmTaskStatusReq.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }
}

