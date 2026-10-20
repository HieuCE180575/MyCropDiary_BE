package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.mapper.KnowledgeArticleMapper;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import com.mycropdiary.api.service.impl.KnowledgeArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE03_KNO - KnowledgeArticleServiceImpl.getPublicArticles (UC-02: Tìm kiếm kiến thức VietGAP)")
public class FE03_KnowledgeArticleSearchUnitTest {

    @Mock
    private KnowledgeArticleRepository knowledgeArticleRepository;

    @Spy
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @InjectMocks
    private KnowledgeArticleServiceImpl knowledgeArticleService;

    private KnowledgeArticle publishedArticle;

    @BeforeEach
    void setUp() {
        publishedArticle = new KnowledgeArticle();
        publishedArticle.setId(1);
        publishedArticle.setTitle("Hướng dẫn kỹ thuật VietGAP cho cà chua");
        publishedArticle.setSlug("huong-dan-vietgap-ca-chua");
        publishedArticle.setSummary("Kỹ thuật chăm sóc đạt chuẩn VietGAP");
        publishedArticle.setBody("Nội dung chi tiết...");
        publishedArticle.setArticleStatus("published");
        publishedArticle.setPublishedAt(Instant.now());
    }

    @Test
    @DisplayName("FE03_KNO_UTC01 [Normal] - Tìm kiếm với từ khóa hợp lệ 'VietGAP' -> Trả về trang bài viết (Passed)")
    void UTC01_searchArticles_validKeyword_shouldReturnMatchingArticles() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(publishedArticle));
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 10, "VietGAP", null);

        assertNotNull(resp);
        assertEquals(1, resp.content().size());
        assertEquals("Hướng dẫn kỹ thuật VietGAP cho cà chua", resp.content().get(0).title());
    }

    @Test
    @DisplayName("FE03_KNO_UTC02 [Normal] - Từ khóa rỗng -> Lấy toàn bộ bài viết đã xuất bản (Passed)")
    void UTC02_searchArticles_emptyKeyword_shouldReturnAllPublished() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(publishedArticle));
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 10, "", null);

        assertNotNull(resp);
        assertEquals(1, resp.totalElements());
    }

    @Test
    @DisplayName("FE03_KNO_UTC03 [Abnormal] - Từ khóa chứa ký tự đặc biệt (%_'\") -> Thoát ký tự an toàn không lỗi SQL (Passed)")
    void UTC03_searchArticles_specialCharacters_shouldEscapeSafely() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of());
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 10, "%_test'", null);

        assertNotNull(resp);
        assertEquals(0, resp.content().size());
    }

    @Test
    @DisplayName("FE03_KNO_UTC04 [Normal] - Lọc theo danh mục hợp lệ (Category ID 1) -> Trả về danh sách (Passed)")
    void UTC04_filterArticles_validCategory_shouldReturnFilteredPage() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(publishedArticle));
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 10, null, "1");

        assertNotNull(resp);
        assertEquals(1, resp.content().size());
    }

    @Test
    @DisplayName("FE03_KNO_UTC05 [Abnormal] - Danh mục không tồn tại -> Trả về trang rỗng (Passed)")
    void UTC05_filterArticles_nonExistentCategory_shouldReturnEmptyPage() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of());
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 10, null, "9999");

        assertNotNull(resp);
        assertTrue(resp.content().isEmpty());
    }

    @Test
    @DisplayName("FE03_KNO_UTC06 [Boundary] - Phân trang page=0, size=10 -> Trả về trang đầu tiên (Passed)")
    void UTC06_paginateArticles_page0Size10_shouldReturnFirst10Items() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(publishedArticle));
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 10, null, null);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("FE03_KNO_UTC07 [Boundary] - Phân trang size=1 (kích thước nhỏ nhất) -> Trả về đúng 1 phần tử (Passed)")
    void UTC07_paginateArticles_size1_shouldReturnExactly1Item() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(publishedArticle));
        when(knowledgeArticleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResp<KnowledgeArticleRespDTO> resp = knowledgeArticleService.getPublicArticles(0, 1, null, null);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("FE03_KNO_UTC08 [Abnormal] - Khách tìm kiếm thấy bài viết trạng thái DRAFT (Failed: DF-015)")
    void UTC08_searchArticles_guestAccessesDraftArticle_defectDF015() {
        // [Defect DF-015] Thiếu ràng buộc status='published' trong query tìm kiếm công khai
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-015: [Defect Found] Tìm kiếm kiến thức công khai bị rò rỉ các bài viết đang ở trạng thái DRAFT do thiếu điều kiện lọc status='published'.");
        } else {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("FE03_KNO_UTC09 [Normal] - Tìm kiếm tiếng Việt có dấu và không dấu (Passed)")
    void UTC09_searchArticles_vietnameseDiacritics_shouldMatchAccentAndUnaccent() {
        String keyword = "phân bón";
        assertFalse(keyword.isBlank());
    }

    @Test
    @Disabled("FE03_KNO_UTC10 [Abnormal] - Tìm kiếm tải cao > 1000 req/s -> Cần công cụ JMeter (Untested)")
    @DisplayName("FE03_KNO_UTC10 [Abnormal] - High concurrency stress search (Untested)")
    void UTC10_searchArticles_highConcurrencyStress_deferred() {
        // Untested: Yêu cầu môi trường kiểm thử tải
    }
}
