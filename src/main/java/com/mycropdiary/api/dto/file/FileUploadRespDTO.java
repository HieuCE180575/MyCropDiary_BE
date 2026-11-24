package com.mycropdiary.api.dto.file;

import java.time.Instant;

/**
 * DTO chứa thông tin kết quả upload file lên Cloudinary / hệ thống lưu trữ.
 */
public record FileUploadRespDTO(
        String url,
        String publicId,
        String originalFilename,
        String resourceType,
        String format,
        Long bytes,
        Instant uploadedAt
) {
    public FileUploadRespDTO(String url, String publicId, String originalFilename, String resourceType, String format, Long bytes) {
        this(url, publicId, originalFilename, resourceType, format, bytes, Instant.now());
    }
}
