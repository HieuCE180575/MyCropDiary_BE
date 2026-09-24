package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.service.KnowledgeArticleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
class KnowledgeArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeArticleService knowledgeArticleService;

    @Test
    @DisplayName("GET /api/v1/knowledge/articles returns public articles list")
    void getPublicArticles_Success() throws Exception {
        KnowledgeArticleResponseDTO dto = new KnowledgeArticleResponseDTO(
                1L,
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

        PageResponse<KnowledgeArticleResponseDTO> pageResponse = new PageResponse<>(List.of(dto), 0, 10, 1L, 1);

        when(knowledgeArticleService.getPublicArticles(0, 10, "tomato", null)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/knowledge/articles")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "tomato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Tomato Cultivation Guide"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/knowledge/articles/{id} returns article detail")
    void getPublicArticleById_Success() throws Exception {
        KnowledgeArticleResponseDTO dto = new KnowledgeArticleResponseDTO(
                1L,
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

        when(knowledgeArticleService.getPublicArticleById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/knowledge/articles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Tomato Cultivation Guide"));
    }

    @Test
    @DisplayName("GET /api/v1/knowledge/articles/{id} returns 404 when not found or not public")
    void getPublicArticleById_NotFound() throws Exception {
        when(knowledgeArticleService.getPublicArticleById(99L))
                .thenThrow(new ResourceNotFoundException("Knowledge article not found with id: 99"));

        mockMvc.perform(get("/api/v1/knowledge/articles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Knowledge article not found with id: 99"));
    }
}
