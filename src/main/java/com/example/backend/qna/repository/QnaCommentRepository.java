package com.example.backend.qna.repository;

import com.example.backend.qna.entity.QnaComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [QnA 댓글 레포지토리]
 * - QNA_COMMENTS 테이블 CRUD를 담당
 */
public interface QnaCommentRepository extends JpaRepository<QnaComment, Long> {

    // 특정 QnA 게시글의 댓글 목록 조회 (오래된 순)
    List<QnaComment> findByQnaPost_IdOrderByCreatedAtAsc(Long qnaPostId);

    // QnA 게시글 삭제 시 댓글도 일괄 삭제
    void deleteByQnaPost_Id(Long qnaPostId);
}
