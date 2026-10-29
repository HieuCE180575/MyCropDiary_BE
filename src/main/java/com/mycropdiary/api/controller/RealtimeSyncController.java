package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RestControllerAdvice
@RequestMapping("/api/v1/realtime")
@Tag(name = "00. Đồng bộ thời gian thực (Realtime Sync)", description = "API kiểm tra phiên bản dữ liệu đồng bộ ngầm giữa các vai trò")
@SecurityRequirement(name = "BearerAuthentication")
public class RealtimeSyncController implements ResponseBodyAdvice<Object> {

    private final AtomicLong version = new AtomicLong(1L);
    private final AtomicReference<Instant> updatedAt = new AtomicReference<>(Instant.now());
    private final AtomicReference<String> lastPath = new AtomicReference<>("");

    public void recordMutation(String path) {
        version.incrementAndGet();
        updatedAt.set(Instant.now());
        lastPath.set(path != null ? path : "");
    }

    @Operation(summary = "Lấy phiên bản đồng bộ hiện tại", description = "Dùng cho đồng bộ ngầm (silent realtime sync) giữa các vai trò mà không cần tải lại trang.")
    @GetMapping("/version")
    public ApiResp<Map<String, Object>> getVersion() {
        return ApiResp.ok("OK", Map.of(
                "version", version.get(),
                "updatedAt", updatedAt.get().toString(),
                "lastPath", lastPath.get()
        ));
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        HttpMethod method = request.getMethod();
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH || method == HttpMethod.DELETE) {
            String path = request.getURI().getPath();
            if (path != null && !path.startsWith("/api/v1/auth/")) {
                if (!(body instanceof ApiResp<?> apiResp) || apiResp.success()) {
                    recordMutation(path);
                }
            }
        }
        return body;
    }
}

