package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Tiện ích hỗ trợ trích xuất thông tin người dùng đang đăng nhập từ Spring Security Context.
 */
@Component
public class SecurityUtils {
    private final AppUserRepository appUserRepository;

    public SecurityUtils(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Optional<Long> getCurrentUserIdOptional() {
        // 1. Check HTTP header X-User-Id for testing/development flexibility
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserId = request.getHeader("X-User-Id");
            if (headerUserId != null && !headerUserId.isBlank()) {
                try {
                    return Optional.of(Long.parseLong(headerUserId.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Check SecurityContext Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long userId) {
                return Optional.of(userId);
            }
            if (principal instanceof Number num) {
                return Optional.of(num.longValue());
            }

            String username = null;
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof String principalName) {
                username = principalName;
            }

            if (username != null) {
                try {
                    return Optional.of(Long.parseLong(username));
                } catch (NumberFormatException ignored) {
                    return appUserRepository.findByEmailIgnoreCase(username).map(AppUser::getId);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy ID người dùng hiện tại từ đối tượng Authentication trong SecurityContext.
     * Mặc định trả về 1L nếu không có người dùng đăng nhập (hỗ trợ dev/skeleton test).
     */
    // [AI_CHANGE] Root cause: Cần tương thích cả bảo mật JWT lẫn dev test khi gọi getCurrentUserId()
    // [AI_CHANGE] Mechanism: Đọc từ getCurrentUserIdOptional(), fallback 1L cho legacy calls, ném AccessDeniedException khi dùng getRequiredCurrentUserId()
    public Long getCurrentUserId() {
        return getCurrentUserIdOptional().orElse(1L);
    }

    public Long getRequiredCurrentUserId() {
        return getCurrentUserIdOptional().orElseThrow(() ->
                new AccessDeniedException("Yêu cầu xác thực tài khoản người dùng"));
    }

    public AppUser getRequiredCurrentUser() {
        Long userId = getRequiredCurrentUserId();
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng ID: " + userId));
    }
}
