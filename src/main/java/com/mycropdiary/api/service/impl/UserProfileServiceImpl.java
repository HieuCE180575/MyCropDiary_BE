package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.user.ChangePasswordReq;
import com.mycropdiary.api.dto.user.UpdateProfileReq;
import com.mycropdiary.api.dto.user.UserProfileResp;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileServiceImpl(UserRepository userRepository,
                                  AccountTokenRepository accountTokenRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileResp getProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));
        return UserProfileResp.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResp updateProfile(Integer userId, UpdateProfileReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        user.setFullName(request.fullName().trim());

        if (request.phone() != null) {
            String cleanPhone = request.phone().trim();
            user.setPhone(cleanPhone.isEmpty() ? null : cleanPhone);
        }

        if (request.avatarUrl() != null) {
            String cleanAvatar = request.avatarUrl().trim();
            user.setAvatarUrl(cleanAvatar.isEmpty() ? null : cleanAvatar);
        }

        User saved = userRepository.save(user);
        log.info("Cập nhật hồ sơ thành công cho userId: {}", userId);
        return UserProfileResp.fromEntity(saved);
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, ChangePasswordReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Vô hiệu hóa các refresh token cũ để buộc làm mới phiên bảo mật
        List<AccountToken> oldTokens = accountTokenRepository
                .findAllByUser_IdAndTokenTypeAndUsedAtIsNull(userId, "refresh");
        oldTokens.forEach(AccountToken::revoke);
        accountTokenRepository.saveAll(oldTokens);

        log.info("Đổi mật khẩu thành công cho userId: {}", userId);
    }
}
