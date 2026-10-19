package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.KnowledgeArticleMapper;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import com.mycropdiary.api.service.impl.KnowledgeArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeArticleServiceTest {

    @Mock
    private KnowledgeArticleRepository knowledgeArticleRepository;

    @Spy
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @InjectMocks
    private KnowledgeArticleServiceImpl knowledgeArticleService;

    private KnowledgeArticle publicArticle;
    private KnowledgeArticle draftArticle;

    private final Integer publicArticleId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer draftArticleId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);

    @BeforeEach
    void setUp() {
        publicArticle = new KnowledgeArticle();
        publicArticle.setId(publicArticleId);
        publicArticle.setTitle("Tomato Cultivation Guide");
        publicArticle.setSlug("tomato-cultivation-guide");
        publicArticle.setSummary("Complete guide to growing tomatoes");
        publicArticle.setBody("Detailed content...");
        publicArticle.setArticleStatus("published");
        publicArticle.setPublishedAt(Instant.now());

        draftArticle = new KnowledgeArticle();
        draftArticle.setId(draftArticleId);
        draftArticle.setTitle("Draft Article");
        draftArticle.setSlug("draft-article");
        draftArticle.setArticleStatus("draft");
    }

    @Test
    @DisplayName("UC-01: Guest can view public knowledge articles with pagination")
    void getPublicArticles_Success() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(publicArticle), PageRequest.of(0, 10), 1);
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> response = knowledgeArticleService.getPublicArticles(0, 10, null, null);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Tomato Cultivation Guide", response.content().get(0).title());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(1, response.totalElements());
    }

    @Test
    @DisplayName("UC-02: Guest can get public article detail by ID")
    void getPublicArticleById_Success() {
        when(knowledgeArticleRepository.findById(publicArticleId)).thenReturn(Optional.of(publicArticle));

        KnowledgeArticleRespDTO response = knowledgeArticleService.getPublicArticleById(publicArticleId);

        assertNotNull(response);
        assertEquals(publicArticleId, response.id());
        assertEquals("Tomato Cultivation Guide", response.title());
        assertEquals(KnowledgeStatus.PUBLISHED, response.status());
    }

    @Test
    @DisplayName("UC-02: Throw ResourceNotFoundException when article is DRAFT")
    void getPublicArticleById_DraftArticle_ThrowsException() {
        when(knowledgeArticleRepository.findById(draftArticleId)).thenReturn(Optional.of(draftArticle));

        assertThrows(ResourceNotFoundException.class, () -> knowledgeArticleService.getPublicArticleById(draftArticleId));
    }

    @Test
    @DisplayName("UC-02: Throw ResourceNotFoundException when article ID does not exist")
    void getPublicArticleById_NotFound_ThrowsException() {
        Integer nonExistentId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
        when(knowledgeArticleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> knowledgeArticleService.getPublicArticleById(nonExistentId));
    }
}
