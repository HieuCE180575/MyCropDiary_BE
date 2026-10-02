package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Plot;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.PlotRepository;
import com.mycropdiary.api.repository.ProductionAreaRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Lớp hỗ trợ bảo mật và phân quyền truy cập theo phạm vi trang trại (Farm Scope & Staff Area Scope).
 */
@Component
public class FarmAccessGuard {
    private final FarmMemberRepository farmMemberRepository;
    private final AppUserRepository appUserRepository;
    private final StaffAreaAssignmentRepository staffAreaAssignmentRepository;
    private final ProductionAreaRepository productionAreaRepository;
    private final PlotRepository plotRepository;

    public FarmAccessGuard(FarmMemberRepository farmMemberRepository,
                           AppUserRepository appUserRepository,
                           StaffAreaAssignmentRepository staffAreaAssignmentRepository,
                           ProductionAreaRepository productionAreaRepository,
                           PlotRepository plotRepository) {
        this.farmMemberRepository = farmMemberRepository;
        this.appUserRepository = appUserRepository;
        this.staffAreaAssignmentRepository = staffAreaAssignmentRepository;
        this.productionAreaRepository = productionAreaRepository;
        this.plotRepository = plotRepository;
    }

    /**
     * Kiểm tra người dùng có phải là Quản trị viên hệ thống (System ADMIN) hay không.
     *
     * @param userId ID người dùng
     * @return true nếu là ADMIN hệ thống, ngược lại false
     */
    public boolean isSystemAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return appUserRepository.findById(userId)
                .map(user -> "ADMIN".equalsIgnoreCase(user.getSystemRole()))
                .orElse(false);
    }

    /**
     * Lấy vai trò hoạt động của người dùng trong một trang trại cụ thể (ADMIN, OWNER, STAFF hoặc null).
     *
     * @param userId ID người dùng
     * @param farmId ID trang trại
     * @return Tên vai trò (ADMIN, OWNER, STAFF) hoặc null nếu không thuộc trang trại
     */
    public String getUserRoleInFarm(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return "ADMIN";
        }
        return farmMemberRepository.findActiveRoleInFarm(farmId, userId)
                .orElse(null);
    }

    /**
     * Yêu cầu người dùng phải là thành viên đang hoạt động (Active Farm Member) của trang trại.
     *
     * @param userId ID người dùng
     * @param farmId ID trang trại
     * @return Đối tượng FarmMember nếu hợp lệ (null nếu là System ADMIN)
     * @throws AccessDeniedException nếu không có quyền truy cập
     */
    public FarmMember requireFarmMember(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return null; // System admin được miễn trừ yêu cầu thành viên
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Access denied: You are not an active member of this farm (Farm ID: " + farmId + ")"));
    }

    public FarmMember requireFarmAccess(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "ACTIVE").orElse(null);
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền truy cập vào trang trại này (yêu cầu là thành viên ACTIVE)"));
    }

    /**
     * Yêu cầu người dùng phải là Chủ trang trại (Farm OWNER) hoặc Quản trị hệ thống (System ADMIN).
     *
     * @param userId ID người dùng
     * @param farmId ID trang trại
     * @return Đối tượng FarmMember nếu hợp lệ
     * @throws AccessDeniedException nếu không phải OWNER hoặc ADMIN
     */
    public FarmMember requireOwnerOrAdmin(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return null;
        }
        FarmMember member = requireFarmMember(userId, farmId);
        if (!"OWNER".equalsIgnoreCase(member.getFarmRole())) {
            throw new AccessDeniedException("Access denied: Only Farm OWNER or System ADMIN can perform this action");
        }
        return member;
    }

    public FarmMember requireFarmOwner(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "ACTIVE").orElse(null);
        }
        FarmMember member = requireFarmAccess(userId, farmId);
        if (member == null || !member.isOwner()) {
            throw new AccessDeniedException("Thao tác yêu cầu quyền OWNER của trang trại");
        }
        return member;
    }

    /**
     * BR-07: Yêu cầu người dùng có quyền truy cập vào Vùng sản xuất cụ thể.
     * OWNER/ADMIN có quyền truy cập toàn bộ; STAFF phải được phân công trong StaffAreaAssignment.
     *
     * @param userId ID người dùng
     * @param farmId ID trang trại
     * @param productionAreaId ID vùng sản xuất
     * @throws AccessDeniedException nếu không được phân công
     */
    public void requireProductionAreaAccess(Long userId, Long farmId, Long productionAreaId) {
        if (isSystemAdmin(userId)) {
            return;
        }
        FarmMember member = requireFarmMember(userId, farmId);
        if ("OWNER".equalsIgnoreCase(member.getFarmRole())) {
            return; // OWNER có quyền xem tất cả vùng sản xuất
        }
        boolean isAssigned = staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(member.getId(), productionAreaId);
        if (!isAssigned) {
            throw new AccessDeniedException("Access denied: Staff is not assigned to Production Area ID: " + productionAreaId);
        }
    }

    public ProductionArea requireProductionAreaAccess(Long userId, Long productionAreaId) {
        ProductionArea area = productionAreaRepository.findById(productionAreaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực sản xuất ID: " + productionAreaId));

        if (isSystemAdmin(userId)) {
            return area;
        }

        FarmMember member = requireFarmAccess(userId, area.getFarm().getId());
        if (member != null && member.isOwner()) {
            return area;
        }

        if (member == null) {
            throw new AccessDeniedException("Bạn không phải thành viên của trang trại này");
        }

        // Nếu là STAFF, kiểm tra xem có phân công khu vực active không
        boolean isAssigned = staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndIsActiveTrue(member.getId(), productionAreaId);
        if (!isAssigned) {
            throw new AccessDeniedException("Nhân viên không được phân công phụ trách khu vực sản xuất này");
        }

        return area;
    }

    public Plot requirePlotAccess(Long userId, Long plotId) {
        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thửa đất ID: " + plotId));

        requireProductionAreaAccess(userId, plot.getProductionArea().getId());
        return plot;
    }

    public boolean isOwner(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return true;
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "ACTIVE")
                .map(FarmMember::isOwner)
                .orElse(false);
    }
}
