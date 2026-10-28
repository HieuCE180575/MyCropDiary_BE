package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.CreateKnowledgeArticleReq;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.dto.knowledge.UpdateKnowledgeArticleReq;
import com.mycropdiary.api.dto.knowledge.UpdateKnowledgeStatusReq;
import com.mycropdiary.api.service.admin.AdminKnowledgeArticleService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/knowledge/articles")
@Tag(name = "03. Quản trị - Cẩm nang bài viết kiến thức (Admin)", description = "APIs dành cho Quản trị viên quản lý, đăng tải và chỉnh sửa bài viết kiến thức nông nghiệp")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeArticleController {

    private final AdminKnowledgeArticleService adminKnowledgeArticleService;
    private final SecurityUtils securityUtils;

    public AdminKnowledgeArticleController(AdminKnowledgeArticleService adminKnowledgeArticleService,
                                          SecurityUtils securityUtils) {
        this.adminKnowledgeArticleService = adminKnowledgeArticleService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Danh sách bài viết kiến thức (Admin)",
            description = "Admin xem danh sách bài viết trong hệ thống, bao gồm bài nháp (draft), đã đăng (published) và lưu trữ (archived). Hỗ trợ tìm kiếm và lọc.")
    @GetMapping
    public ApiResp<PageResp<KnowledgeArticleRespDTO>> getAdminArticles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResp<KnowledgeArticleRespDTO> page = adminKnowledgeArticleService.getAdminArticles(status, category, search, pageable);
        return ApiResp.ok("Danh sách bài viết kiến thức (Quản trị)", page);
    }

    @Operation(summary = "Xem chi tiết bài viết (Admin)",
            description = "Admin xem chi tiết bài viết theo ID (kể cả bài viết đang ở trạng thái nháp hoặc lưu trữ).")
    @GetMapping("/{id}")
    public ApiResp<KnowledgeArticleRespDTO> getAdminArticleById(@PathVariable Integer id) {
        KnowledgeArticleRespDTO article = adminKnowledgeArticleService.getAdminArticleById(id);
        return ApiResp.ok("Chi tiết bài viết kiến thức", article);
    }

    @Operation(summary = "Tạo mới bài viết kiến thức (Admin)",
            description = "Tạo bài viết mới. Tự động sinh đường dẫn slug chuẩn SEO từ tiêu đề bài viết nếu không nhập slug.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<KnowledgeArticleRespDTO> createArticle(@Valid @RequestBody CreateKnowledgeArticleReq request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        KnowledgeArticleRespDTO created = adminKnowledgeArticleService.createArticle(adminUserId, request);
        return ApiResp.ok("Tạo bài viết kiến thức thành công", created);
    }

    @Operation(summary = "Cập nhật bài viết kiến thức (Admin)",
            description = "Cập nhật tiêu đề, nội dung, chuyên mục, ảnh bìa, thẻ tags và nguồn tham khảo của bài viết.")
    @PutMapping("/{id}")
    public ApiResp<KnowledgeArticleRespDTO> updateArticle(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateKnowledgeArticleReq request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        KnowledgeArticleRespDTO updated = adminKnowledgeArticleService.updateArticle(adminUserId, id, request);
        return ApiResp.ok("Cập nhật bài viết kiến thức thành công", updated);
    }

    @Operation(summary = "Đổi trạng thái bài viết (Admin)",
            description = "Cập nhật trạng thái bài viết: draft (nháp), published (xuất bản) hoặc archived (lưu trữ). Khi xuất bản lần đầu sẽ tự động gán ngày xuất bản.")
    @PatchMapping("/{id}/status")
    public ApiResp<KnowledgeArticleRespDTO> updateArticleStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateKnowledgeStatusReq request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        KnowledgeArticleRespDTO updated = adminKnowledgeArticleService.updateArticleStatus(adminUserId, id, request);
        return ApiResp.ok("Cập nhật trạng thái bài viết thành công", updated);
    }

    @Operation(summary = "Xóa bài viết kiến thức (Admin)",
            description = "Xóa vĩnh viễn bài viết kiến thức khỏi hệ thống theo ID.")
    @DeleteMapping("/{id}")
    public ApiResp<Void> deleteArticle(@PathVariable Integer id) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        adminKnowledgeArticleService.deleteArticle(adminUserId, id);
        return ApiResp.ok("Xóa bài viết kiến thức thành công", null);
    }
}

