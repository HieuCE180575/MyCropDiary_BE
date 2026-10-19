package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import com.mycropdiary.api.exception.GlobalExceptionHandler;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.service.KnowledgeArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Test cho tầng Controller của KnowledgeArticle (API Endpoint Testing).
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeArticleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KnowledgeArticleService knowledgeArticleService;

    @InjectMocks
    private KnowledgeArticleController knowledgeArticleController;

    private final Integer articleId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(knowledgeArticleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Test API lấy danh sách bài viết public thành công (HTTP Status 200 OK)
     */
    @Test
    @DisplayName("GET /api/v1/knowledge/articles - Trả về danh sách bài viết công khai thành công")
    void getPublicArticles_Success() throws Exception {
        KnowledgeArticleRespDTO dto = new KnowledgeArticleRespDTO(
                articleId,
                "Tomato Cultivation Guide",
                "tomato-cultivation-guide",
                "Guide summary",
                "Full content",
                "tomato",
                "https://example.com/guide",
                KnowledgeStatus.PUBLISHED,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        PageResp<KnowledgeArticleRespDTO> pageResponse = new PageResp<>(List.of(dto), 0, 10, 1L, 1, true);

        when(knowledgeArticleService.getPublicArticles(0, 10, "tomato", null)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/knowledge/articles")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "tomato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(articleId.toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("Tomato Cultivation Guide"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    /**
     * Test API xem chi tiết bài viết public thành công (HTTP Status 200 OK)
     */
    @Test
    @DisplayName("GET /api/v1/knowledge/articles/{id} - Trả về chi tiết bài viết thành công")
    void getPublicArticleById_Success() throws Exception {
        KnowledgeArticleRespDTO dto = new KnowledgeArticleRespDTO(
                articleId,
                "Tomato Cultivation Guide",
                "tomato-cultivation-guide",
                "Guide summary",
                "Full content",
                "tomato",
                "https://example.com/guide",
                KnowledgeStatus.PUBLISHED,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(knowledgeArticleService.getPublicArticleById(articleId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/knowledge/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(articleId.toString()))
                .andExpect(jsonPath("$.data.title").value("Tomato Cultivation Guide"));
    }

    /**
     * Test API xem chi tiết bài viết thất bại khi ID không tồn tại hoặc bị ẩn (HTTP Status 404 Not Found)
     */
    @Test
    @DisplayName("GET /api/v1/knowledge/articles/{id} - Trả về lỗi 404 khi không tìm thấy hoặc bài viết bị ẩn")
    void getPublicArticleById_NotFound() throws Exception {
        Integer unknownId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
        when(knowledgeArticleService.getPublicArticleById(unknownId))
                .thenThrow(new ResourceNotFoundException("Knowledge article not found with id: " + unknownId));

        mockMvc.perform(get("/api/v1/knowledge/articles/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Knowledge article not found with id: " + unknownId));
    }
}
