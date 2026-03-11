package com.example.backend.album.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
[파일 역할]
- [BACK][API] 앨범 생성 요청 바디(JSON)를 받는 DTO입니다.

[관련 기능(화면/요청)]
- API: POST /api/albums
- 프론트: CreatePhotoAlbumPage.jsx

[실행 흐름(순서)]
1) 프론트가 사용자 입력값을 JSON으로 전송
2) Controller가 CreateAlbumRequest로 역직렬화
3) Service로 전달되어 저장 로직 수행
*/
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlbumRequest {

    // [API] 작성자 ID
    @NotNull
    private Long userId;

    // [API] 앨범 제목
    @NotBlank
    private String title;

    // [API] 앨범 본문
    private String bodyText;

    // [API] 기록 날짜 (YYYY-MM-DD)
    @NotNull
    private LocalDate recordDate;

    // [API] 공개 범위 (PUBLIC/FRIENDS/PRIVATE)
    @NotBlank
    private String visibility;

    // [API] 레이아웃 타입
    @NotBlank
    private String layoutType;

    // [API] 업로드 완료된 photoId 목록
    @NotNull
    private List<Long> photoIds;

    // [API] photoIds와 1:1 대응되는 slotIndex 목록
    @NotNull
    private List<Integer> slotIndexes;

    // [API] 태그 문자열 목록
    private List<String> tags;
}
