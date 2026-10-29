package com.mycropdiary.api.dto.cultivation;

public record CropCategoryResp(
        Integer id,
        String categoryName,
        String description,
        String imageUrl
) {
}

