package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.training.CreateTrainingRecordReq;
import com.mycropdiary.api.dto.training.TrainingRecordResp;
import com.mycropdiary.api.dto.training.UpdateTrainingRecordReq;
import com.mycropdiary.api.service.cultivation.TrainingRecordService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "07. Đào tạo nhân lực (Training Records)", description = "APIs quản lý hồ sơ tập huấn, đào tạo kỹ thuật VietGAP và điểm danh công nhân")
@SecurityRequirement(name = "BearerAuthentication")
public class TrainingRecordController {

    private final TrainingRecordService trainingService;
    private final SecurityUtils securityUtils;

    public TrainingRecordController(TrainingRecordService trainingService, SecurityUtils securityUtils) {
        this.trainingService = trainingService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Create Training Record", description = "Tạo mới hồ sơ đào tạo / tập huấn VietGAP cho công nhân trang trại.")
    @PostMapping("/farms/{farmId}/training-records")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<TrainingRecordResp> createTrainingRecord(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateTrainingRecordReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        TrainingRecordResp response = trainingService.createTrainingRecord(currentUserId, farmId, request);
        return ApiResp.ok("Ghi nhận hồ sơ đào tạo thành công.", response);
    }

    @Operation(summary = "View Training History", description = "Xem lịch sử các khóa đào tạo, tập huấn kỹ thuật của trang trại (hỗ trợ tìm kiếm theo từ khóa và lọc theo khoảng thời gian).")
    @GetMapping("/farms/{farmId}/training-records")
    public ApiResp<PageResp<TrainingRecordResp>> getTrainingHistory(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "trainingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<TrainingRecordResp> response = trainingService.getTrainingHistory(currentUserId, farmId, keyword, fromDate, toDate, pageable);
        return ApiResp.ok("Lấy lịch sử đào tạo thành công.", response);
    }

    @Operation(summary = "View Training Record Details", description = "Xem chi tiết hồ sơ đào tạo kèm danh sách điểm danh và ghi chú kết quả công nhân.")
    @GetMapping("/training-records/{trainingRecordId}")
    public ApiResp<TrainingRecordResp> getTrainingRecordDetails(@PathVariable Integer trainingRecordId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        TrainingRecordResp response = trainingService.getTrainingRecordDetails(currentUserId, trainingRecordId);
        return ApiResp.ok("Chi tiết hồ sơ đào tạo.", response);
    }

    @Operation(summary = "Update Training Record", description = "Cập nhật thông tin hồ sơ đào tạo và danh sách công nhân tham gia.")
    @PutMapping("/training-records/{trainingRecordId}")
    public ApiResp<TrainingRecordResp> updateTrainingRecord(
            @PathVariable Integer trainingRecordId,
            @Valid @RequestBody UpdateTrainingRecordReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        TrainingRecordResp response = trainingService.updateTrainingRecord(currentUserId, trainingRecordId, request);
        return ApiResp.ok("Cập nhật hồ sơ đào tạo thành công.", response);
    }

    @Operation(summary = "Delete Training Record", description = "Xóa hồ sơ đào tạo.")
    @DeleteMapping("/training-records/{trainingRecordId}")
    public ApiResp<Void> deleteTrainingRecord(@PathVariable Integer trainingRecordId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        trainingService.deleteTrainingRecord(currentUserId, trainingRecordId);
        return ApiResp.ok("Đã xóa hồ sơ đào tạo.", null);
    }
}
