package com.example.backend.notification.dto;

import java.time.LocalDateTime;

import com.example.backend.notification.entity.NotificationEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class NotificationDto {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String message;

    @JsonProperty("read")
    private boolean isRead;

    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환 팩토리 메서드
     */
    public static NotificationDto from(NotificationEntity entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .isRead(entity.getIsRead() != null && entity.getIsRead() == 1)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
