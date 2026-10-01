package com.mycropdiary.api.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * DTO bọc dữ liệu phản hồi phân trang chung (Paginated Response).
 *
 * @param <T> Kiểu dữ liệu của các phần tử trong trang
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /**
     * Alias method for items to maintain compatibility with Spring Page naming convention.
     */
    @JsonIgnore
    public List<T> content() {
        return items;
    }

    @JsonIgnore
    public List<T> getContent() {
        return items;
    }

    /**
     * Tạo PageResponse từ đối tượng Page của Spring Data.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Tạo PageResponse từ đối tượng Page của Spring Data kèm hàm ánh xạ kiểu phần tử.
     */
    public static <T, R> PageResponse<R> map(Page<T> page, java.util.function.Function<T, R> mapper) {
        List<R> mappedContent = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}

