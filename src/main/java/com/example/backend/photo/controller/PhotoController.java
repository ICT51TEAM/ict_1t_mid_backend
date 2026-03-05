package com.example.backend.photo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.global.dto.ApiErrorResponse;
import com.example.backend.photo.dto.PhotoDto;
import com.example.backend.photo.dto.PhotoUploadResponse;
import com.example.backend.photo.service.PhotoService;

import lombok.RequiredArgsConstructor;

/*
[파일 역할]
- [BACK][API] 사진 업로드 요청을 받아서 Service에 위임하고 결과를 응답합니다.

[관련 기능(화면/요청)]
- 프론트: CreatePhotoAlbumPage.jsx 완료 버튼 흐름 1단계
- API: POST /api/photos/upload

[실행 흐름(순서)]
1) files, userId를 RequestParam으로 받기
2) PhotoService.uploadPhotos() 호출
3) photos 배열 응답 반환
4) 오류 시 code/message/details 표준 에러 반환
*/
@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    // [BACK][API]
    // - 어디서 호출? : frontend/src/api/photoService.js
    // - 입력값 : files(MultipartFile[]), userId(Long)
    // - 출력값 : { photos: [{photoId, photoUrl, thumbUrl}] }
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhotos(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("userId") Long userId) {
        try {
            List<PhotoDto> photos = photoService.uploadPhotos(files, userId);
            return ResponseEntity.ok(PhotoUploadResponse.builder().photos(photos).build());
        } catch (IllegalArgumentException e) {
            Map<String, Object> details = new HashMap<>();
            details.put("userId", userId);
            details.put("fileCount", files == null ? 0 : files.size());

            return ResponseEntity.badRequest().body(
                    ApiErrorResponse.builder()
                            .code("VALIDATION_ERROR")
                            .message(e.getMessage())
                            .details(details)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiErrorResponse.builder()
                            .code("INTERNAL_SERVER_ERROR")
                            .message("사진 업로드 중 오류가 발생했습니다.")
                            .details(Map.of())
                            .build());
        }
    }
}
