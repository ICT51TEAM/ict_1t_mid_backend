package com.example.backend.album.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
[파일 역할]
- [BACK][API] 앨범 생성 성공 응답 DTO입니다.

[관련 기능(화면/요청)]
- API: POST /api/albums
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlbumResponse {

    // [API] 생성된 앨범 ID
    private Long albumId;

    // [API] 사용자 안내 메시지
    private String message;
}
