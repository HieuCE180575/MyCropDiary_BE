package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;

public interface KnowledgeArticleService {

    PageResponse<KnowledgeArticleResponseDTO> getPublicArticles(int page, int size, String keyword, String categoryId);

    KnowledgeArticleResponseDTO getPublicArticleById(Long id);
}
