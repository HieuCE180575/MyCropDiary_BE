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
     * @return ID người dùng đang đăng nhập (Mặc định 1L nếu đang chạy dev/skeleton chưa xác thực)
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            String username = null;
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof String principalName) {
                username = principalName;
            }

            if (username != null) {
                return appUserRepository.findByEmailIgnoreCase(username)
                        .map(AppUser::getId)
                        .orElse(1L);
            }
        }
        return 1L; // Giá trị mặc định hỗ trợ chạy thử nghiệm dev skeleton
    }
}
