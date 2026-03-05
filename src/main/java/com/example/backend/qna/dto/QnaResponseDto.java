package com.example.backend.qna.dto;

import com.example.backend.qna.entity.QnaPost;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaResponseDto {
    private Long id;
    private String title;
    private String content;
    private String username;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QnaCommentResponseDto> comments;
    private int commentCount;

    public static QnaResponseDto fromEntity(QnaPost qna) {
        return QnaResponseDto.builder()
                .id(qna.getId())
                .title(qna.getTitle())
                .content(qna.getContent())
                .username(qna.getUser() != null ? qna.getUser().getUsername() : "익명")
                .userId(qna.getUser() != null ? qna.getUser().getId() : null)
                .createdAt(qna.getCreatedAt())
                .updatedAt(qna.getUpdatedAt())
                .comments(null)
                .commentCount(0)
                .build();
    }
}
