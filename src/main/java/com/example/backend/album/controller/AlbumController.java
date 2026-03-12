package com.example.backend.album.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.album.dto.AlbumDetailResponse;
import com.example.backend.album.dto.AlbumFeedItemResponse;
import com.example.backend.album.dto.CreateAlbumRequest;
import com.example.backend.album.dto.CreateAlbumResponse;
import com.example.backend.album.dto.LatestFrinendAlbumDto;
import com.example.backend.album.service.AlbumService;
import com.example.backend.global.dto.ApiErrorResponse;

import lombok.RequiredArgsConstructor;

/*
[파일 역할]
- [BACK][API] 앨범 관련 요청(생성/상세/레이아웃 코드 조회)을 처리합니다.

[관련 기능(화면/요청)]
- 프론트: CreatePhotoAlbumPage.jsx 완료 버튼 흐름
- 프론트: AlbumDetailPage.jsx 상세 조회
- API: POST /api/albums
- API: GET /api/albums/layout-types
- API: GET /api/albums/{albumId}

[실행 흐름(순서)]
1) 요청값을 DTO로 수신
2) AlbumService에 위임
3) 성공 시 정상 응답 반환
4) 실패 시 표준 에러 응답(code/message/details) 반환
*/
@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    // [BACK][API]
    // - 어디서 호출? : 프론트 CreatePhotoAlbumPage 초기 로딩/디버깅
    // - 입력값 : 없음
    // - 출력값 : DB에 등록된 layoutType 코드 목록
    @GetMapping("/layout-types")
    public ResponseEntity<?> getLayoutTypes() {
        try {
            List<String> layoutTypes = albumService.getAvailableLayoutTypes();
            return ResponseEntity.ok(Map.of("layoutTypes", layoutTypes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("레이아웃 코드 조회 중 오류가 발생했습니다.")
                            .details(Map.of())
                            .build());
        }
    }

    // [BACK][API]
    // - 어디서 호출? : frontend/src/api/postService.js (getPosts)
    // - 입력값 : type, friendsOnly, tag
    // - 출력값 : 피드 카드 목록
    @GetMapping("/feed")
    public ResponseEntity<?> getAlbumFeed(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String tag,
            Authentication authentication) {
        try {
            List<AlbumFeedItemResponse> items = albumService.getAlbumFeed(type, visibility, tag, authentication);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("앨범 피드 조회 중 오류가 발생했습니다.")
                            .details(Map.of(
                                    "type", type == null ? "" : type,
                                    "visibility", visibility == null ? "" : visibility,
                                    "tag", tag == null ? "" : tag,
                                    "reason", e.getMessage() == null ? "null" : e.getMessage()))
                            .build());
        }
    }

    // [BACK][API]
    // - 어디서 호출? : frontend/src/api/albumService.js
    // - 입력값 : CreateAlbumRequest JSON
    // - 출력값 : CreateAlbumResponse 또는 ApiErrorResponse
    @PostMapping
    public ResponseEntity<?> createAlbum(@RequestBody CreateAlbumRequest request) {
        try {
            CreateAlbumResponse response = albumService.createAlbum(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiErrorResponse.builder()
                            .code("VALIDATION_ERROR")
                            .message(e.getMessage())
                            .details(Map.of("title", request.getTitle()))
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("앨범 생성 중 오류가 발생했습니다.")
                            .details(Map.of(
                                    "exception", e.getClass().getName(),
                                    "reason", e.getMessage() == null ? "null" : e.getMessage()))
                            .build());
        }
    }

    // [BACK][API]
    // - 어디서 호출? : frontend/src/pages/feed/AlbumDetailPage.jsx
    // - 입력값 : albumId(PathVariable)
    // - 출력값 : AlbumDetailResponse 또는 ApiErrorResponse
    @GetMapping("/{albumId}")
    public ResponseEntity<?> getAlbumDetail(@PathVariable Long albumId) {
        try {
            AlbumDetailResponse response = albumService.getAlbumDetail(albumId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiErrorResponse.builder()
                            .code("VALIDATION_ERROR")
                            .message(e.getMessage())
                            .details(Map.of("albumId", albumId))
                            .build());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiErrorResponse.builder()
                            .code("NOT_FOUND")
                            .message(e.getMessage())
                            .details(Map.of("albumId", albumId))
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("앨범 상세 조회 중 오류가 발생했습니다.")
                            .details(Map.of(
                                    "albumId", albumId,
                                    "exception", e.getClass().getName(),
                                    "reason", e.getMessage() == null ? "null" : e.getMessage()))
                            .build());
        }
    }

    // [BACK][API]
    // - 어디서 호출? : frontend postService.updatePost()
    // - 입력값 : albumId(PathVariable), body(title, bodyText, visibility),
    // Authentication
    // - 출력값 : AlbumDetailResponse 또는 ApiErrorResponse
    @PutMapping("/{albumId}")
    public ResponseEntity<?> updateAlbum(
            @PathVariable Long albumId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            String title = body.get("title");
            String bodyText = body.get("bodyText");
            String visibility = body.get("visibility");
            AlbumDetailResponse response = albumService.updateAlbum(albumId, title, bodyText, visibility,
                    authentication);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiErrorResponse.builder()
                            .code("FORBIDDEN")
                            .message(e.getMessage())
                            .details(Map.of("albumId", albumId))
                            .build());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiErrorResponse.builder()
                            .code("NOT_FOUND")
                            .message(e.getMessage())
                            .details(Map.of("albumId", albumId))
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("앨범 수정 중 오류가 발생했습니다.")
                            .details(Map.of("albumId", albumId,
                                    "reason", e.getMessage() == null ? "null" : e.getMessage()))
                            .build());
        }
    }

    // [BACK][API]
    // - 어디서 호출? : frontend postService.deletePost()
    // - 입력값 : albumId(PathVariable), Authentication
    // - 출력값 : 200 OK with message
    @DeleteMapping("/{albumId}")
    public ResponseEntity<?> deleteAlbum(
            @PathVariable Long albumId,
            Authentication authentication) {
        try {
            albumService.deleteAlbum(albumId, authentication);
            return ResponseEntity.ok(Map.of("message", "앨범이 삭제되었습니다."));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiErrorResponse.builder()
                            .code("FORBIDDEN")
                            .message(e.getMessage())
                            .details(Map.of("albumId", albumId))
                            .build());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiErrorResponse.builder()
                            .code("NOT_FOUND")
                            .message(e.getMessage())
                            .details(Map.of("albumId", albumId))
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("앨범 삭제 중 오류가 발생했습니다.")
                            .details(Map.of("albumId", albumId,
                                    "reason", e.getMessage() == null ? "null" : e.getMessage()))
                            .build());
        }
    }

}
