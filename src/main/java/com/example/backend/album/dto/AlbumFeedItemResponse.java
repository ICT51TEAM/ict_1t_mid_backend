package com.example.backend.album.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
[파일 역할]
- [BACK][DTO] 피드 목록 카드 1건 응답 형식을 정의합니다.

[관련 기능(화면/요청)]
- API: GET /api/albums/feed
- 화면: FeedPage.jsx, CreatePage.jsx

[프론트 호환 키]
- id, type, imageUrl, title, author, preview, tags, badges, date
- routeType: 클릭 시 어떤 상세 경로로 갈지 식별("album")
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumFeedItemResponse {

    // [응답] 피드 카드 ID(앨범 ID와 동일)
    private Long id;

    // [응답] 카드 타입(기존 피드 호환: photo/text)
    private String type;

    // [응답] 상세 경로 분기용 타입("album")
    private String routeType;

    // [응답] 대표 이미지 URL
    private String imageUrl;

    // [응답] 카드 제목
    private String title;

    // [응답] 작성자 닉네임
    private String author;

    // [응답] 작성자 ID
    private Long authorId;

    // [응답] 카드 본문 미리보기
    private String preview;

    // [응답] 태그 목록
    private List<String> tags;

    // [응답] 달개 목록(현재 앨범 피드는 빈 배열)
    private List<AlbumFeedBadgeDto> badges;

    // [응답] 화면 표시용 날짜 문자열
    private String date;
}
