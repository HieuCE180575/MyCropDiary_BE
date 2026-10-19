package com.mycropdiary.api.dto.common;

import java.time.Instant;

public record ApiResp<T>(boolean success, String message, T data, Instant timestamp) {
    public static <T> ApiResp<T> ok(String message, T data) {
        return new ApiResp<>(true, message, data, Instant.now());
    }

    public static ApiResp<Void> error(String message) {
        return new ApiResp<>(false, message, null, Instant.now());
    }
}
