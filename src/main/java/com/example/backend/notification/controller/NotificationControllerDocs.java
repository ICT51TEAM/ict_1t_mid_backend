package com.example.backend.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "알림 API", description = "사용자의 알림(Notification) 조회 및 관리 API입니다.")
public interface NotificationControllerDocs {

    @Operation(summary = "전체 알림 조회", description = "현재 사용자의 모든 알림 목록을 가져옵니다.")
    ResponseEntity<?> getNotifications(Authentication authentication);

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 사용자가 읽지 않은 알림의 개수를 반환합니다.")
    ResponseEntity<?> getUnreadCount(Authentication authentication);

    @Operation(summary = "특정 알림 읽음 처리", description = "지정한 알림을 읽음 상태로 변경합니다.")
    ResponseEntity<?> markAsRead(Authentication authentication, @PathVariable Long id);

    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    ResponseEntity<?> markAllAsRead(Authentication authentication);

    @Operation(summary = "특정 알림 삭제", description = "지정한 알림을 삭제합니다.")
    ResponseEntity<?> deleteNotification(Authentication authentication, @PathVariable Long id);

    @Operation(summary = "모든 알림 삭제", description = "사용자의 모든 알림을 삭제합니다.")
    ResponseEntity<?> deleteAllNotifications(Authentication authentication);
}
