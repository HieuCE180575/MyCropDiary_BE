package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Tiện ích hỗ trợ trích xuất thông tin người dùng đang đăng nhập từ Spring Security Context.
 */
@Component
public class SecurityUtils {
    private final AppUserRepository appUserRepository;

    public SecurityUtils(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Lấy ID người dùng hiện tại từ đối tượng Authentication trong SecurityContext.
     *
     * @return ID người dùng đang đăng nhập
     * @throws IllegalStateException nếu chưa đăng nhập hoặc không xác định được userId
     */
    // [AI_CHANGE] Root cause: Fallback return 1L cực kỳ nguy hiểm — user anonymous bị coi như user ID 1 (có thể là Admin)
    // [AI_CHANGE] Mechanism: Bỏ fallback, ném exception rõ ràng khi chưa xác thực; JwtAuthenticationFilter
    //             đặt principal là Long userId nên ưu tiên đọc trực tiếp từ principal
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập.");
        }

        Object principal = authentication.getPrincipal();

        // [AI_CHANGE] JwtAuthenticationFilter đặt principal = userId (Long)
        if (principal instanceof Long userId) {
            return userId;
        }

        // [AI_CHANGE] Fallback: nếu principal là String (userId dạng String từ JWT subject)
        if (principal instanceof String principalStr) {
            try {
                return Long.parseLong(principalStr);
            } catch (NumberFormatException ignored) {
                // Không phải số -> thử tìm theo email
            }

            return appUserRepository.findByEmailIgnoreCase(principalStr)
                    .map(AppUser::getId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy tài khoản cho principal: " + principalStr));
        }

        if (principal instanceof UserDetails userDetails) {
            return appUserRepository.findByEmailIgnoreCase(userDetails.getUsername())
                    .map(AppUser::getId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy tài khoản cho username: " + userDetails.getUsername()));
        }

        throw new IllegalStateException("Không thể xác định ID người dùng từ SecurityContext.");
    }
}
