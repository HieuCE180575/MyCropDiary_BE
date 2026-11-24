package com.mycropdiary.api.dto.cultivation;

public record CropCategoryRespDTO(
        Integer id,
        String categoryName,
        String description,
        String imageUrl,
        String status
) {
    public CropCategoryRespDTO(Integer id, String categoryName, String description, String imageUrl) {
        this(id, categoryName, description, imageUrl, "ACTIVE");
    }
}

