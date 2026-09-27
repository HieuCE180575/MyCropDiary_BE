package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.service.FarmService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý REST API cho module Farm Core (Trang trại, Thành viên, Vùng sản xuất).
 */
@RestController
@RequestMapping("/api/v1/farms")
@Tag(name = "02. Trang trại (Farms)", description = "APIs quản lý trang trại, thành viên và vùng sản xuất")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmController {
    private final FarmService farmService;
    private final SecurityUtils securityUtils;

    public FarmController(FarmService farmService, SecurityUtils securityUtils) {
        this.farmService = farmService;
        this.securityUtils = securityUtils;
    }

    /**
     * Tạo mới trang trại. Người tạo sẽ tự động được cấp quyền OWNER.
     *
     * @param request Thông tin trang trại mới
     * @return Kết quả tạo trang trại kèm vai trò OWNER
     */
    @Operation(summary = "Tạo mới trang trại", description = "Tạo mới trang trại. Người tạo sẽ tự động được cấp quyền OWNER.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmResponse> createFarm(@Valid @RequestBody CreateFarmRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.createFarm(currentUserId, request);
        return ApiResponse.ok("Farm created successfully", response);
    }

    /**
     * Lấy chi tiết thông tin trang trại theo ID. Yêu cầu quyền truy cập (Thành viên trang trại hoặc Admin).
     *
     * @param farmId Mã ID trang trại
     * @return Chi tiết trang trại kèm vai trò của người dùng hiện tại
     */
    @Operation(summary = "Lấy chi tiết trang trại", description = "Lấy chi tiết thông tin trang trại theo ID.")
    @GetMapping("/{farmId}")
    public ApiResponse<FarmResponse> getFarmById(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.getFarmById(currentUserId, farmId);
        return ApiResponse.ok("Farm details", response);
    }

    /**
     * Tìm kiếm và lọc danh sách trang trại mà người dùng có quyền truy cập (Có phân trang).
     *
     * @param keyword Từ khóa tìm kiếm theo tên hoặc mã trang trại
     * @param status Lọc theo trạng thái (ACTIVE, INACTIVE)
     * @param province Lọc theo tỉnh/thành phố
     * @param pageable Cấu hình phân trang
     * @return Danh sách trang trại phân trang
     */
    @Operation(summary = "Tìm kiếm trang trại", description = "Tìm kiếm và lọc danh sách trang trại mà người dùng có quyền truy cập (Có phân trang).")
    @GetMapping
    public ApiResponse<PageResponse<FarmSummaryResponse>> searchFarms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        PageResponse<FarmSummaryResponse> page = farmService.searchFarms(currentUserId, keyword, status, province, pageable);
        return ApiResponse.ok("Accessible farms list", page);
    }

    /**
     * Cập nhật thông tin trang trại. Yêu cầu quyền Farm OWNER hoặc System ADMIN.
     *
     * @param farmId Mã ID trang trại
     * @param request Thông tin cập nhật
     * @return Chi tiết trang trại sau khi cập nhật
     */
    @Operation(summary = "Cập nhật trang trại", description = "Cập nhật thông tin trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PutMapping("/{farmId}")
    public ApiResponse<FarmResponse> updateFarm(
            @PathVariable Long farmId,
            @Valid @RequestBody UpdateFarmRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.updateFarm(currentUserId, farmId, request);
        return ApiResponse.ok("Farm updated successfully", response);
    }

    /**
     * Vô hiệu hóa (Soft Delete) trang trại. Yêu cầu quyền Farm OWNER hoặc System ADMIN.
     *
     * @param farmId Mã ID trang trại
     * @return Thông báo thành công
     */
    @Operation(summary = "Xóa trang trại (Soft delete)", description = "Vô hiệu hóa trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @DeleteMapping("/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteFarm(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        farmService.deleteFarm(currentUserId, farmId);
        return ApiResponse.ok("Farm deactivated successfully", null);
    }

    /**
     * Lấy danh sách thành viên đang hoạt động trong trang trại.
     *
     * @param farmId Mã ID trang trại
     * @return Danh sách thành viên trang trại
     */
    @Operation(summary = "Danh sách thành viên trang trại", description = "Lấy danh sách thành viên đang hoạt động trong trang trại.")
    @GetMapping("/{farmId}/members")
    public ApiResponse<List<FarmMemberResponse>> getFarmMembers(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<FarmMemberResponse> members = farmService.getFarmMembers(currentUserId, farmId);
        return ApiResponse.ok("Farm members list", members);
    }

    /**
     * Thêm thành viên mới vào trang trại. Kiểm tra quy tắc 1 Active Owner. Yêu cầu quyền OWNER hoặc ADMIN.
     *
     * @param farmId Mã ID trang trại
     * @param request Thông tin thành viên mới (Email, Vai trò, Chức danh)
     * @return Thông tin thành viên vừa thêm
     */
    @Operation(summary = "Thêm thành viên vào trang trại", description = "Thêm thành viên mới vào trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PostMapping("/{farmId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmMemberResponse> addMember(
            @PathVariable Long farmId,
            @Valid @RequestBody AddFarmMemberRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmMemberResponse member = farmService.addMember(currentUserId, farmId, request);
        return ApiResponse.ok("Farm member added successfully", member);
    }

    /**
     * Xóa/Vô hiệu hóa thành viên khỏi trang trại. Kiểm tra quy tắc bảo vệ Owner duy nhất.
     *
     * @param farmId Mã ID trang trại
     * @param memberId Mã ID thành viên cần xóa
     * @return Thông báo thành công
     */
    @Operation(summary = "Xóa thành viên khỏi trang trại", description = "Xóa/Vô hiệu hóa thành viên khỏi trang trại.")
    @DeleteMapping("/{farmId}/members/{memberId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long farmId,
            @PathVariable Long memberId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        farmService.removeMember(currentUserId, farmId, memberId);
        return ApiResponse.ok("Farm member removed successfully", null);
    }

    /**
     * Phân công nhân viên (STAFF) quản lý một Vùng sản xuất. Yêu cầu quyền OWNER hoặc ADMIN.
     *
     * @param farmId Mã ID trang trại
     * @param memberId Mã ID thành viên (Staff)
     * @param request Thông tin phân công (Vùng sản xuất, Ngày bắt đầu, Ngày kết thúc)
     * @return Thông tin phân công sau khi lưu
     */
    @Operation(summary = "Phân công nhân viên vào vùng sản xuất", description = "Phân công nhân viên quản lý vùng sản xuất. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PostMapping("/{farmId}/members/{memberId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffAreaAssignmentResponse> assignStaffToArea(
            @PathVariable Long farmId,
            @PathVariable Long memberId,
            @Valid @RequestBody AssignStaffAreaRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        StaffAreaAssignmentResponse assignment = farmService.assignStaffToArea(currentUserId, farmId, memberId, request);
        return ApiResponse.ok("Staff assigned to production area successfully", assignment);
    }

    /**
     * Tạo Vùng sản xuất mới trong trang trại. Kiểm tra tổng diện tích không vượt quá diện tích trang trại.
     *
     * @param farmId Mã ID trang trại
     * @param request Thông tin vùng sản xuất (Mã, Tên, Diện tích)
     * @return Thông tin vùng sản xuất vừa tạo
     */
    @Operation(summary = "Tạo vùng sản xuất mới", description = "Tạo Vùng sản xuất mới trong trang trại. Kiểm tra tổng diện tích không vượt quá diện tích trang trại.")
    @PostMapping("/{farmId}/production-areas")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductionAreaResponse> createProductionArea(
            @PathVariable Long farmId,
            @Valid @RequestBody CreateProductionAreaRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ProductionAreaResponse response = farmService.createProductionArea(currentUserId, farmId, request);
        return ApiResponse.ok("Production area created successfully", response);
    }

    /**
     * Lấy danh sách vùng sản xuất của trang trại.
     * STAFF chỉ nhìn thấy các vùng được phân công; OWNER/ADMIN nhìn thấy tất cả.
     *
     * @param farmId Mã ID trang trại
     * @return Danh sách vùng sản xuất theo phạm vi quyền
     */
    @Operation(summary = "Lấy danh sách vùng sản xuất", description = "Lấy danh sách vùng sản xuất của trang trại theo quyền truy cập.")
    @GetMapping("/{farmId}/production-areas")
    public ApiResponse<List<ProductionAreaResponse>> getProductionAreas(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<ProductionAreaResponse> areas = farmService.getProductionAreas(currentUserId, farmId);
        return ApiResponse.ok("Production areas list", areas);
    }
}
