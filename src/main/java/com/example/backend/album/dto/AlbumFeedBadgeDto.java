package com.example.backend.album.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
[파일 역할]
- [BACK][DTO] 피드 카드의 달개 요약 형식입니다.

[관련 기능(화면/요청)]
- API: GET /api/albums/feed
- 화면: FeedPage.jsx, CreatePage.jsx

[비고]
- 현재 앨범 피드에서는 달개를 집계하지 않아 기본적으로 빈 배열을 반환합니다.
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumFeedBadgeDto {

    // [응답] 달개 이모지
    private String emoji;

    // [응답] 달개 이름
    private String name;

    // [응답] 달개 개수
    private Integer count;
}
