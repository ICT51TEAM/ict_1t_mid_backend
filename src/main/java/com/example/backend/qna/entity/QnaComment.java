package com.example.backend.qna.entity;

import com.example.backend.global.entity.BaseEntity;
import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * [QnA 댓글 엔티티 (QnaComment)]
 * - QnA 게시글에 달리는 댓글(답변)을 저장하는 테이블
 * - 오라클 DB의 QNA_COMMENTS 테이블과 매핑
 * - BaseEntity를 상속하여 createdAt, updatedAt 자동 관리
 */
@Entity
@Table(name = "QNA_COMMENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaComment extends BaseEntity {

    @Id
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POST_ID", nullable = false)
    private QnaPost qnaPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @Column(name = "CONTENT", nullable = false, length = 100)
    private String content;
}
