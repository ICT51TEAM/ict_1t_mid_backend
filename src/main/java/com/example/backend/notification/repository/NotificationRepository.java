package com.example.backend.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.notification.entity.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * 특정 유저의 알림 목록 조회 (최신순)
     */
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 유저의 읽지 않은 알림 개수
     */
    long countByUserIdAndIsRead(Long userId, Integer isRead);

    /**
     * 특정 유저의 모든 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = 1 WHERE n.user.id = :userId AND n.isRead = 0")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 특정 알림 하나를 읽음 처리
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = 1 WHERE n.id = :notificationId")
    int markAsRead(@Param("notificationId") Long notificationId);

    /**
     * 특정 유저의 알림 전체 삭제
     */
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
