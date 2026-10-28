package com.mycropdiary.api.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeArticleReq(
        @NotBlank(message = "Tiêu đề bài viết không được để trống")
        @Size(max = 220, message = "Tiêu đề không được vượt quá 220 ký tự")
        String title,

        String slug,

        String summary,

        @NotBlank(message = "Nội dung bài viết không được để trống")
        String body,

        String coverImageUrl,

        String category,

        String tags,

        String sourceUrl,

        String status // draft, published, archived (mặc định draft nếu null)
) {
}

