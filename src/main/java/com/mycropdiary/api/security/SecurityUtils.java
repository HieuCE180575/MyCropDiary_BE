package com.mycropdiary.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


public final class SecurityUtils {
    private SecurityUtils() { }

    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Integer userId) {
            return userId;
        }
        return Integer.valueOf(principal.toString());
    }
}
