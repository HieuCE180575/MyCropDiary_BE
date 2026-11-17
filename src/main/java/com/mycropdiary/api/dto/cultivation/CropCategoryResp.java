package com.mycropdiary.api.dto.cultivation;

public record CropCategoryResp(
        Integer id,
        String categoryName,
        String description,
        String imageUrl,
        String status
) {
    public CropCategoryResp(Integer id, String categoryName, String description, String imageUrl) {
        this(id, categoryName, description, imageUrl, "ACTIVE");
    }
}


