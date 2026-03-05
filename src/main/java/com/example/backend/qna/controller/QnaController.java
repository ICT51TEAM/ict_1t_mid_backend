package com.example.backend.qna.controller;

import com.example.backend.qna.dto.QnaCommentRequestDto;
import com.example.backend.qna.dto.QnaCommentResponseDto;
import com.example.backend.qna.dto.QnaRequestDto;
import com.example.backend.qna.dto.QnaResponseDto;
import com.example.backend.qna.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @GetMapping
    public ResponseEntity<Page<QnaResponseDto>> getQnas(
            @PageableDefault(size = 5, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(qnaService.getQnaList(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QnaResponseDto> getQna(@PathVariable Long id) {
        return ResponseEntity.ok(qnaService.getQnaDetail(id));
    }

    @PostMapping
    public ResponseEntity<QnaResponseDto> createQna(
            @RequestBody QnaRequestDto requestDto,
            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(qnaService.createQna(requestDto, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QnaResponseDto> updateQna(
            @PathVariable Long id,
            @RequestBody QnaRequestDto requestDto,
            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(qnaService.updateQna(id, requestDto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQna(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        qnaService.deleteQna(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // ── 댓글 엔드포인트 ──

    /** 특정 QnA의 댓글 목록 조회 — GET /qna/{id}/comments */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<QnaCommentResponseDto>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(qnaService.getComments(id));
    }

    /** 댓글 생성 — POST /qna/{id}/comments (인증 필요) */
    @PostMapping("/{id}/comments")
    public ResponseEntity<QnaCommentResponseDto> createComment(
            @PathVariable Long id,
            @RequestBody QnaCommentRequestDto requestDto,
            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(qnaService.createComment(id, requestDto, authentication.getName()));
    }

    /** 댓글 삭제 — DELETE /qna/comments/{commentId} (인증 필요) */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(401).build();
        qnaService.deleteComment(commentId, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
