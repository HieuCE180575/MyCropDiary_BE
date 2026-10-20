package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReq;
import com.mycropdiary.api.dto.admin.UserResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<UserResp> getUsers(String status, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<User> page = userRepository.searchUsers(cleanStatus, cleanSearch, pageable);
        return PageResp.from(page.map(UserResp::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResp getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + userId));
        return UserResp.fromEntity(user);
    }

    @Override
    public UserResp updateUserStatus(Integer adminUserId, Integer targetUserId, UpdateUserStatusReq request) {
        String newStatus = request.status().trim().toLowerCase();

        if (adminUserId != null && adminUserId.equals(targetUserId) && "locked".equalsIgnoreCase(newStatus)) {
            throw new BadRequestException("Quản trị viên không thể tự khóa tài khoản của chính mình.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + targetUserId));

        user.setAccountStatus(newStatus);
        User savedUser = userRepository.save(user);

        return UserResp.fromEntity(savedUser);
    }
}
