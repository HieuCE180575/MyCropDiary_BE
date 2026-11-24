package com.mycropdiary.api.dto.cultivation;

public record CreateCropCategoryReqDTO(String categoryName, String description, String imageUrl, String status) {
    public CreateCropCategoryReqDTO(String categoryName, String description, String imageUrl) {
        this(categoryName, description, imageUrl, "ACTIVE");
    }
}
