package com.mycropdiary.api.controller.compliance;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.compliance.CreateComplaintReqDTO;
import com.mycropdiary.api.dto.compliance.CustomerComplaintRespDTO;
import com.mycropdiary.api.dto.compliance.RecallProductReqDTO;
import com.mycropdiary.api.dto.compliance.ResolveComplaintReqDTO;
import com.mycropdiary.api.service.compliance.CustomerComplaintService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-48: Customer Complaints & Product Recall (Mục 4.10 VietGAP)", description = "Ghi nhận khiếu nại (Staff + Owner), Giải quyết & Thu hồi sản phẩm (Owner-only)")
@SecurityRequirement(name = "BearerAuthentication")
@RestController
@RequestMapping("/api/v1")
public class CustomerComplaintController {

    private final CustomerComplaintService complaintService;
    private final SecurityUtils securityUtils;

    public CustomerComplaintController(CustomerComplaintService complaintService, SecurityUtils securityUtils) {
        this.complaintService = complaintService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "UC-48: Xem danh sách khiếu nại & thu hồi sản phẩm (Staff + Owner)")
    @GetMapping("/farms/{farmId}/complaints")
    public ApiResp<List<CustomerComplaintRespDTO>> getComplaints(@PathVariable Integer farmId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Danh sách khiếu nại khách hàng", complaintService.getComplaints(userId, farmId));
    }

    @Operation(summary = "UC-48: Ghi nhận khiếu nại khách hàng mới (Staff + Owner)")
    @PostMapping("/farms/{farmId}/complaints")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<CustomerComplaintRespDTO> createComplaint(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateComplaintReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Ghi nhận khiếu nại thành công", complaintService.createComplaint(userId, farmId, request));
    }

    @Operation(summary = "UC-48.4 (Owner-only): Cập nhật trạng thái & phương án giải quyết khiếu nại")
    @PutMapping("/complaints/{complaintId}/resolve")
    public ApiResp<CustomerComplaintRespDTO> resolveComplaint(
            @PathVariable Integer complaintId,
            @Valid @RequestBody ResolveComplaintReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Cập nhật kết quả xử lý khiếu nại thành công", complaintService.resolveComplaint(userId, complaintId, request));
    }

    @Operation(summary = "UC-48.5 (Owner-only): Ghi nhận lệnh thu hồi lô sản phẩm (BR-100)")
    @PutMapping("/complaints/{complaintId}/recall")
    public ApiResp<CustomerComplaintRespDTO> recordProductRecall(
            @PathVariable Integer complaintId,
            @Valid @RequestBody RecallProductReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Ghi nhận thu hồi sản phẩm thành công", complaintService.recordProductRecall(userId, complaintId, request));
    }

    @Operation(summary = "UC-48 (Owner-only): Xóa hồ sơ khiếu nại")
    @DeleteMapping("/complaints/{complaintId}")
    public ApiResp<Void> deleteComplaint(@PathVariable Integer complaintId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        complaintService.deleteComplaint(userId, complaintId);
        return ApiResp.ok("Đã xóa hồ sơ khiếu nại", null);
    }
}
