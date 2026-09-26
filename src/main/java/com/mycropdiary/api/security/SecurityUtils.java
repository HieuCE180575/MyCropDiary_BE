package com.mycropdiary.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// [AI_CHANGE] Root cause: Nhiều Service cần lấy userId của người dùng đang đăng nhập từ SecurityContext
// [AI_CHANGE] Mechanism: Đọc principal (kiểu Long) đã được JwtAuthenticationFilter đặt vào context
public final class SecurityUtils {
    private SecurityUtils() { }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        // Trường hợp principal là String (từ UserDetails username = userId.toString())
        return Long.parseLong(principal.toString());
    }
}
