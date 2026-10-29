package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.service.cultivation.FarmTaskService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "06. Phân công & Quản lý công việc (Farm Tasks)", description = "APIs tạo việc, phân công lao động, theo dõi tiến độ và trạng thái công việc")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmTaskController {

    private final FarmTaskService farmTaskService;
    private final SecurityUtils securityUtils;

    public FarmTaskController(FarmTaskService farmTaskService, SecurityUtils securityUtils) {
        this.farmTaskService = farmTaskService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Lấy danh sách công việc của trang trại", description = "Lấy danh sách công việc phân công theo trang trại kèm bộ lọc mùa vụ, khu vực, trạng thái và độ ưu tiên.")
    @GetMapping("/api/v1/farms/{farmId}/tasks")
    public ApiResp<PageResp<FarmTaskResp>> getFarmTasks(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 50, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<FarmTaskResp> response = farmTaskService.getFarmTasks(
                currentUserId, farmId, cropSeasonId, zoneId, status, priority, keyword, pageable);
        return ApiResp.ok("Lấy danh sách công việc thành công.", response);
    }

    @Operation(summary = "Tạo mới công việc và phân công lao động", description = "Tạo công việc mới cho nông trại và gán cho các nhân sự phụ trách.")
    @PostMapping("/api/v1/farms/{farmId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmTaskResp> createTask(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateFarmTaskReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmTaskResp response = farmTaskService.createTask(currentUserId, farmId, request);
        return ApiResp.ok("Tạo công việc thành công.", response);
    }

    @Operation(summary = "Xem chi tiết công việc", description = "Xem thông tin chi tiết một công việc theo ID.")
    @GetMapping("/api/v1/tasks/{taskId}")
    public ApiResp<FarmTaskResp> getTaskById(@PathVariable Integer taskId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmTaskResp response = farmTaskService.getTaskById(currentUserId, taskId);
        return ApiResp.ok("Lấy chi tiết công việc thành công.", response);
    }

    @Operation(summary = "Cập nhật công việc", description = "Chỉnh sửa nội dung, phân công, lịch trình của công việc.")
    @PutMapping("/api/v1/tasks/{taskId}")
    public ApiResp<FarmTaskResp> updateTask(
            @PathVariable Integer taskId,
            @Valid @RequestBody UpdateFarmTaskReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmTaskResp response = farmTaskService.updateTask(currentUserId, taskId, request);
        return ApiResp.ok("Cập nhật công việc thành công.", response);
    }

    @Operation(summary = "Cập nhật trạng thái công việc", description = "Chuyển nhanh trạng thái công việc (planned -> in_progress -> completed -> cancelled).")
    @PatchMapping("/api/v1/tasks/{taskId}/status")
    public ApiResp<FarmTaskResp> updateTaskStatus(
            @PathVariable Integer taskId,
            @Valid @RequestBody UpdateFarmTaskStatusReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmTaskResp response = farmTaskService.updateTaskStatus(currentUserId, taskId, request);
        return ApiResp.ok("Cập nhật trạng thái công việc thành công.", response);
    }

    @Operation(summary = "Xóa công việc", description = "Xóa bỏ một công việc nông trại.")
    @DeleteMapping("/api/v1/tasks/{taskId}")
    public ApiResp<Void> deleteTask(@PathVariable Integer taskId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        farmTaskService.deleteTask(currentUserId, taskId);
        return ApiResp.ok("Xóa công việc thành công.", null);
    }
}

