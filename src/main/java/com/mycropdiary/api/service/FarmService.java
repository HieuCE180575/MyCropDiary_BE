package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ cốt lõi cho trang trại (Farm Core).
 */
public interface FarmService {
    /**
     * Tạo mới một trang trại và tự động gán tài khoản tạo làm OWNER.
     *
     * @param currentUserId ID người dùng thực hiện tạo
     * @param request Yêu cầu tạo trang trại
     * @return Thông tin trang trại vừa tạo
     */
    FarmResponse createFarm(Long currentUserId, CreateFarmRequest request);

    /**
     * Lấy thông tin chi tiết trang trại theo ID kèm vai trò người dùng trong trang trại.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @return Thông tin trang trại chi tiết
     */
    FarmResponse getFarmById(Long currentUserId, Long farmId);

    /**
     * Tìm kiếm và phân trang danh sách các trang trại người dùng có quyền truy cập.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param keyword Từ khóa tìm kiếm theo tên hoặc mã
     * @param status Trạng thái hoạt động
     * @param province Tỉnh/Thành phố
     * @param pageable Thông tin phân trang
     * @return Danh sách trang trại phân trang
     */
    PageResponse<FarmSummaryResponse> searchFarms(Long currentUserId, String keyword, String status, String province, Pageable pageable);

    /**
     * Lấy toàn bộ danh sách trang trại người dùng có quyền truy cập (không phân trang).
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @return Danh sách tóm tắt các trang trại
     */
    List<FarmSummaryResponse> findAllAccessibleFarms(Long currentUserId);

    /**
     * Cập nhật thông tin trang trại. Yêu cầu quyền OWNER hoặc ADMIN.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @param request Thông tin cập nhật
     * @return Thông tin trang trại sau cập nhật
     */
    FarmResponse updateFarm(Long currentUserId, Long farmId, UpdateFarmRequest request);

    /**
     * Vô hiệu hóa (Soft Delete) trang trại. Yêu cầu quyền OWNER hoặc ADMIN.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     */
    void deleteFarm(Long currentUserId, Long farmId);

    /**
     * Thêm thành viên mới vào trang trại. Kiểm tra quy tắc tối đa 1 Active OWNER.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @param request Thông tin thành viên mới
     * @return Thông tin thành viên sau khi thêm
     */
    FarmMemberResponse addMember(Long currentUserId, Long farmId, AddFarmMemberRequest request);

    /**
     * Lấy danh sách thành viên đang hoạt động trong trang trại.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @return Danh sách thành viên
     */
    List<FarmMemberResponse> getFarmMembers(Long currentUserId, Long farmId);

    /**
     * Vô hiệu hóa thành viên khỏi trang trại. Bảo vệ không cho xóa OWNER duy nhất.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @param memberId ID bản ghi thành viên cần xóa
     */
    void removeMember(Long currentUserId, Long farmId, Long memberId);

    /**
     * Phân công nhân viên (STAFF) quản lý Vùng sản xuất.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @param memberId ID thành viên (Staff)
     * @param request Thông tin phân công
     * @return Kết quả phân công
     */
    StaffAreaAssignmentResponse assignStaffToArea(Long currentUserId, Long farmId, Long memberId, AssignStaffAreaRequest request);

    /**
     * Tạo Vùng sản xuất mới trong trang trại. Kiểm tra tổng diện tích không vượt quá diện tích farm.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @param request Thông tin vùng sản xuất mới
     * @return Kết quả tạo vùng sản xuất
     */
    ProductionAreaResponse createProductionArea(Long currentUserId, Long farmId, CreateProductionAreaRequest request);

    /**
     * Lấy danh sách vùng sản xuất của trang trại theo Scope phân quyền của người dùng.
     *
     * @param currentUserId ID người dùng thực hiện yêu cầu
     * @param farmId ID trang trại
     * @return Danh sách vùng sản xuất
     */
    List<ProductionAreaResponse> getProductionAreas(Long currentUserId, Long farmId);
}
