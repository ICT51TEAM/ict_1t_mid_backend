package com.example.backend.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.notification.dto.NotificationDto;
import com.example.backend.notification.entity.NotificationEntity;
import com.example.backend.notification.repository.NotificationRepository;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<NotificationDto> getNotifications(Long userId) {
        List<NotificationEntity> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(entity -> NotificationDto.from(entity)).toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, 0);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void createNotification(Long userId, String type, String title, String message) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    // ──────────────────────────────────────────────────────
    // [시스템 알림 사용 안내]
    // 공지사항이나 시스템 안내를 보내야 할 때 아래처럼 호출하세요:
    //
    // notificationService.createNotification(
    // userId, // 알림 받을 사용자 ID
    // "SYSTEM", // 알림 타입
    // "공지사항", // 알림 제목
    // "공지 내용..." // 알림 내용
    // );
    //
    // 예시 활용처:
    // - 회원가입 축하 알림 → AuthService에서 호출
    // - 전체 공지 → 관리자 API에서 전체 유저에게 반복 호출
    // ──────────────────────────────────────────────────────
}
