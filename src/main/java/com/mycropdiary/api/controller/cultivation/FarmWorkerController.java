package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.worker.FarmWorkerReq;
import com.mycropdiary.api.dto.worker.FarmWorkerResp;
import com.mycropdiary.api.service.cultivation.FarmWorkerService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/workers")
@Tag(name = "15. Hồ sơ công nhân nông trại (Worker Records)", description = "APIs quản lý hồ sơ nhân công lao động, công nhật và phân công theo chuẩn VietGAP")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmWorkerController {

    private final FarmWorkerService workerService;
    private final SecurityUtils securityUtils;

    public FarmWorkerController(FarmWorkerService workerService, SecurityUtils securityUtils) {
        this.workerService = workerService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách hồ sơ công nhân", description = "Lấy danh sách hồ sơ công nhân lao động của trang trại, lọc theo trạng thái và tìm kiếm.")
    @GetMapping
    public ApiResp<List<FarmWorkerResp>> getWorkersByFarm(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<FarmWorkerResp> list = workerService.getWorkersByFarm(currentUserId, farmId, status, keyword);
        return ApiResp.ok("Lấy danh sách hồ sơ công nhân thành công.", list);
    }

    @Operation(summary = "Xem chi tiết hồ sơ công nhân", description = "Xem thông tin chi tiết một công nhân theo ID.")
    @GetMapping("/{workerId}")
    public ApiResp<FarmWorkerResp> getWorkerById(
            @PathVariable Integer farmId,
            @PathVariable Integer workerId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmWorkerResp resp = workerService.getWorkerById(currentUserId, farmId, workerId);
        return ApiResp.ok("Chi tiết hồ sơ công nhân.", resp);
    }

    @Operation(summary = "Tạo mới hồ sơ công nhân", description = "Tạo hồ sơ công nhân lao động mới liên kết với thành viên trang trại.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmWorkerResp> createWorker(
            @PathVariable Integer farmId,
            @Valid @RequestBody FarmWorkerReq req) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmWorkerResp resp = workerService.createWorker(currentUserId, farmId, req);
        return ApiResp.ok("Tạo hồ sơ công nhân thành công.", resp);
    }

    @Operation(summary = "Cập nhật hồ sơ công nhân", description = "Chỉnh sửa mã công nhân, chức danh, trạng thái và ngày vào làm.")
    @PutMapping("/{workerId}")
    public ApiResp<FarmWorkerResp> updateWorker(
            @PathVariable Integer farmId,
            @PathVariable Integer workerId,
            @Valid @RequestBody FarmWorkerReq req) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmWorkerResp resp = workerService.updateWorker(currentUserId, farmId, workerId, req);
        return ApiResp.ok("Cập nhật hồ sơ công nhân thành công.", resp);
    }

    @Operation(summary = "Xóa hồ sơ công nhân", description = "Xóa hồ sơ công nhân lao động khỏi trang trại.")
    @DeleteMapping("/{workerId}")
    public ApiResp<Void> deleteWorker(
            @PathVariable Integer farmId,
            @PathVariable Integer workerId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        workerService.deleteWorker(currentUserId, farmId, workerId);
        return ApiResp.ok("Xóa hồ sơ công nhân thành công.", null);
    }
}

