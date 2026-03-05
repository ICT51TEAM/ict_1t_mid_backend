package com.example.backend.qna.service;

import com.example.backend.qna.dto.QnaCommentRequestDto;
import com.example.backend.qna.dto.QnaCommentResponseDto;
import com.example.backend.qna.dto.QnaRequestDto;
import com.example.backend.qna.dto.QnaResponseDto;
import com.example.backend.qna.entity.QnaComment;
import com.example.backend.qna.entity.QnaPost;
import com.example.backend.qna.repository.QnaCommentRepository;
import com.example.backend.qna.repository.QnaRepository;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final QnaCommentRepository qnaCommentRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public Page<QnaResponseDto> getQnaList(Pageable pageable) {
        return qnaRepository.findAllByOrderByIdDesc(pageable).map(QnaResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public QnaResponseDto getQnaDetail(Long id) {
        QnaPost qnaPost = qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        QnaResponseDto dto = QnaResponseDto.fromEntity(qnaPost);

        // 댓글 목록 포함하여 반환
        List<QnaCommentResponseDto> comments = qnaCommentRepository
                .findByQnaPost_IdOrderByCreatedAtAsc(id)
                .stream()
                .map(QnaCommentResponseDto::fromEntity)
                .collect(Collectors.toList());

        dto.setComments(comments);
        dto.setCommentCount(comments.size());

        return dto;
    }

    @Transactional
    public QnaResponseDto createQna(QnaRequestDto requestDto, String principalName) {
        Long userId = Long.parseLong(principalName);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        QnaPost qnaPost = QnaPost.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .user(user)
                .build();

        return QnaResponseDto.fromEntity(qnaRepository.save(qnaPost));
    }

    @Transactional
    public QnaResponseDto updateQna(Long id, QnaRequestDto requestDto, String principalName) {
        Long userId = Long.parseLong(principalName);
        QnaPost qnaPost = qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!qnaPost.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        qnaPost.setTitle(requestDto.getTitle());
        qnaPost.setContent(requestDto.getContent());
        return QnaResponseDto.fromEntity(qnaPost);
    }

    @Transactional
    public void deleteQna(Long id, String principalName) {
        Long userId = Long.parseLong(principalName);
        QnaPost qnaPost = qnaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!qnaPost.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // 댓글 먼저 삭제 후 QnA 삭제
        qnaCommentRepository.deleteByQnaPost_Id(id);
        qnaRepository.delete(qnaPost);
    }

    // ── 댓글 관련 메서드 ──

    /**
     * 특정 QnA 게시글의 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<QnaCommentResponseDto> getComments(Long qnaId) {
        // QnA 게시글 존재 확인
        qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        return qnaCommentRepository.findByQnaPost_IdOrderByCreatedAtAsc(qnaId)
                .stream()
                .map(QnaCommentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 생성
     */
    @Transactional
    public QnaCommentResponseDto createComment(Long qnaId, QnaCommentRequestDto dto, String principalName) {
        Long userId = Long.parseLong(principalName);

        QnaPost qnaPost = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long nextId = jdbcTemplate.queryForObject(
                "SELECT NVL(MAX(ID), 0) + 1 FROM QNA_COMMENTS", Long.class);

        QnaComment comment = QnaComment.builder()
                .id(nextId)
                .qnaPost(qnaPost)
                .user(user)
                .content(dto.getContent())
                .build();

        return QnaCommentResponseDto.fromEntity(qnaCommentRepository.save(comment));
    }

    /**
     * 댓글 삭제 (작성자만 가능)
     */
    @Transactional
    public void deleteComment(Long commentId, String principalName) {
        Long userId = Long.parseLong(principalName);

        QnaComment comment = qnaCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        qnaCommentRepository.delete(comment);
    }
}
