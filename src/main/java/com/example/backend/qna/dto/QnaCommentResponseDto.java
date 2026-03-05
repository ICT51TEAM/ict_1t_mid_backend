package com.example.backend.qna.dto;

import com.example.backend.qna.entity.QnaComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaCommentResponseDto {
    private Long id;
    private String content;
    private String username;
    private Long userId;
    private LocalDateTime createdAt;

    public static QnaCommentResponseDto fromEntity(QnaComment comment) {
        return QnaCommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .username(comment.getUser() != null ? comment.getUser().getUsername() : "익명")
                .userId(comment.getUser() != null ? comment.getUser().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
