package com.example.backend.album.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
[파일 역할]
- [BACK][DTO] 앨범 상세 응답의 사진 1건 정보를 담는 DTO입니다.

[관련 기능(화면/요청)]
- API: GET /api/albums/{albumId}
- 화면: AlbumDetailPage.jsx

[데이터 의미]
- slotIndex: 앨범 내 배치 순서
- photoUrl/thumbUrl: 프론트 이미지 표시용 경로
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDetailPhotoDto {

    // [응답] 사진 ID
    private Long photoId;

    // [응답] 원본 사진 URL
    private String photoUrl;

    // [응답] 썸네일 URL
    private String thumbUrl;

    // [응답] 앨범 내 슬롯 순서
    private Integer slotIndex;
}
