package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;

/**
 * API Nhật ký quản trị hệ thống (Admin Audit Logs) tổng hợp từ dữ liệu thực tế trong DB.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuthentication")
@Tag(name = "Admin - Nhật ký quản trị (Audit Logs)", description = "Tra cứu lịch sử thao tác quản trị thực tế từ cơ sở dữ liệu")
public class AdminAuditLogController {

    private final FarmRegistrationRepository registrationRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final ChecklistRuleRepository checklistRuleRepository;

    public AdminAuditLogController(
            FarmRegistrationRepository registrationRepository,
            FarmRepository farmRepository,
            UserRepository userRepository,
            ChecklistRuleRepository checklistRuleRepository) {
        this.registrationRepository = registrationRepository;
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.checklistRuleRepository = checklistRuleRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Lấy danh sách nhật ký quản trị từ dữ liệu hệ thống")
    public ApiResp<List<Map<String, Object>>> getAuditLogs() {
        List<Map<String, Object>> events = new ArrayList<>();

        // 1. Lịch sử duyệt / từ chối hồ sơ đăng ký trang trại
        for (FarmRegistration reg : registrationRepository.findAll()) {
            Instant ts = reg.getReviewedAt() != null ? reg.getReviewedAt() : reg.getCreatedAt();
            if (ts == null) ts = Instant.now();
            String status = reg.getStatus() != null ? reg.getStatus().toUpperCase() : "PENDING";
            String action = switch (status) {
                case "APPROVED" -> "Duyệt hồ sơ đăng ký trang trại";
                case "REJECTED" -> "Từ chối hồ sơ đăng ký trang trại";
                case "CANCELLED" -> "Hủy hồ sơ đăng ký trang trại";
                default -> "Tiếp nhận hồ sơ đăng ký trang trại";
            };
            String actor = reg.getReviewedBy() != null && reg.getReviewedBy().getFullName() != null
                    ? reg.getReviewedBy().getFullName()
                    : (reg.getUser() != null ? reg.getUser().getFullName() : "Hệ thống");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "reg-" + reg.getId());
            row.put("name", action);
            row.put("actor", actor);
            row.put("resource", "Hồ sơ #" + reg.getId() + " · " + reg.getFarmName());
            row.put("description", "Trạng thái: " + status
                    + (reg.getRejectionReason() != null && !reg.getRejectionReason().isBlank() ? " — Lý do: " + reg.getRejectionReason() : ""));
            row.put("status", "RECORDED");
            row.put("createdAt", ts.toString());
            events.add(row);
        }

        // 2. Lịch sử tạo / cập nhật trang trại
        for (Farm farm : farmRepository.findAll()) {
            Instant ts = farm.getUpdatedAt() != null ? farm.getUpdatedAt() : farm.getCreatedAt();
            if (ts == null) ts = Instant.now();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "farm-" + farm.getId());
            row.put("name", "Cập nhật hồ sơ trang trại");
            row.put("actor", farm.getCreatedBy() != null ? farm.getCreatedBy().getFullName() : "Quản trị viên");
            row.put("resource", "Trang trại #" + farm.getId() + " · " + farm.getFarmName());
            row.put("description", "Trạng thái trang trại: " + (farm.getFarmStatus() != null ? farm.getFarmStatus().toUpperCase() : "ACTIVE"));
            row.put("status", "RECORDED");
            row.put("createdAt", ts.toString());
            events.add(row);
        }

        // 3. Lịch sử tài khoản người dùng (khóa/mở/cập nhật vai trò)
        for (User u : userRepository.findAll()) {
            Instant ts = u.getUpdatedAt() != null ? u.getUpdatedAt() : u.getCreatedAt();
            if (ts == null) ts = Instant.now();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "user-" + u.getId());
            row.put("name", "locked".equalsIgnoreCase(u.getAccountStatus()) ? "Khóa tài khoản người dùng" : "Cập nhật tài khoản & vai trò");
            row.put("actor", "Quản trị viên");
            row.put("resource", u.getFullName() + " (" + u.getEmail() + ")");
            row.put("description", "Vai trò: " + (u.getSystemRole() != null ? u.getSystemRole().toUpperCase() : "USER")
                    + " · Trạng thái: " + (u.getAccountStatus() != null ? u.getAccountStatus().toUpperCase() : "ACTIVE"));
            row.put("status", "RECORDED");
            row.put("createdAt", ts.toString());
            events.add(row);
        }

        // 4. Lịch sử cấu hình quy tắc VietGAP Master gần đây (tối đa 10 mục mới nhất)
        checklistRuleRepository.findAllSystemMasterRules().stream()
                .sorted(Comparator.comparing(ChecklistRule::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .forEach(rule -> {
                    Instant ts = rule.getCreatedAt() != null ? rule.getCreatedAt() : Instant.now();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", "rule-" + rule.getId());
                    row.put("name", "Cấu hình tiêu chí bảng kiểm VietGAP");
                    row.put("actor", "Quản trị viên");
                    row.put("resource", (rule.getRuleCode() != null ? rule.getRuleCode() + " · " : "") + rule.getRuleName());
                    row.put("description", "Nhóm: " + rule.getCategory() + " · Áp dụng: " + (Boolean.TRUE.equals(rule.getIsActive()) ? "ACTIVE" : "INACTIVE"));
                    row.put("status", "RECORDED");
                    row.put("createdAt", ts.toString());
                    events.add(row);
                });

        events.sort((a, b) -> String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt"))));
        List<Map<String, Object>> limited = events.size() > 100 ? events.subList(0, 100) : events;
        return ApiResp.ok("Danh sách nhật ký quản trị hệ thống", limited);
    }
}
