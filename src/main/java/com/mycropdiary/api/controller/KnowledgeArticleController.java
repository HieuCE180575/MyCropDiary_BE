package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;
import com.mycropdiary.api.service.KnowledgeArticleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    public KnowledgeArticleController(KnowledgeArticleService knowledgeArticleService) {
        this.knowledgeArticleService = knowledgeArticleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<KnowledgeArticleResponseDTO>> getPublicArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String category
    ) {
        String effectiveCategory = categoryId != null ? categoryId : category;
        PageResponse<KnowledgeArticleResponseDTO> result = knowledgeArticleService.getPublicArticles(page, size, keyword, effectiveCategory);
        return ApiResponse.ok("Public knowledge articles retrieved successfully", result);
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeArticleResponseDTO> getPublicArticleById(@PathVariable Long id) {
        KnowledgeArticleResponseDTO article = knowledgeArticleService.getPublicArticleById(id);
        return ApiResponse.ok("Public knowledge article detail retrieved successfully", article);
    }
}
