package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.notification.NotificationListResp;
import com.mycropdiary.api.dto.notification.NotificationResp;
import com.mycropdiary.api.entity.Notification;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.NotificationRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResp getMyNotifications(Integer userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit));
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        // If the user has zero notifications yet, initialize a welcoming system notification
        if (list.isEmpty()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                Notification welcome = new Notification(
                        user,
                        "Chào mừng đến với MyCropDiary",
                        "Hệ thống quản lý trang trại và nhật ký canh tác VietGAP đã sẵn sàng hỗ trợ bạn.",
                        "SYSTEM",
                        "/dashboard"
                );
                notificationRepository.save(welcome);
                list = List.of(welcome);
                unreadCount = 1;
            }
        }

        List<NotificationResp> dtos = list.stream().map(NotificationResp::fromEntity).toList();
        return new NotificationListResp(dtos, unreadCount);
    }

    @Override
    @Transactional
    public NotificationResp markAsRead(Integer userId, Integer notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo hoặc bạn không có quyền truy cập."));
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationResp.fromEntity(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void createNotification(Integer userId, String title, String message, String type, String actionUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Notification notification = new Notification(user, title, message, type, actionUrl);
        notificationRepository.save(notification);
    }
}

