package com.mycropdiary.api.dto.cultivation;

public record UpdateCropCategoryReqDTO(String categoryName, String description, String imageUrl, String status) {
    public UpdateCropCategoryReqDTO(String categoryName, String description, String imageUrl) {
        this(categoryName, description, imageUrl, "ACTIVE");
    }
}
