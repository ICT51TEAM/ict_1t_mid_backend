package com.example.backend.album.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.backend.badge.dto.AlbumDalgaeDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
[파일 역할]
- [BACK][DTO] 앨범 상세 조회 응답 본문을 담는 DTO입니다.

[관련 기능(화면/요청)]
- API: GET /api/albums/{albumId}
- 화면: AlbumDetailPage.jsx

[응답 데이터 구성]
1) 앨범 기본 정보(albumId, title, bodyText, recordDate, visibility, layoutType)
2) 작성자 정보(userId, username)
3) 사진 목록(photos)
4) 태그 목록(tags)
*/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDetailResponse {

    // [응답] 앨범 ID
    private Long albumId;

    // [응답] 작성자 사용자 ID
    private Long userId;

    // [응답] 작성자 닉네임
    private String username;

    // [응답] 앨범 제목
    private String title;

    // [응답] 앨범 본문
    private String bodyText;

    // [응답] 기록 날짜
    private LocalDate recordDate;

    // [응답] 공개 범위(PUBLIC/FRIENDS_ONLY/PRIVATE)
    private String visibility;

    // [응답] 레이아웃 타입
    private String layoutType;

    // [응답] 생성 시각
    private LocalDateTime createdAt;

    // [응답] 사진 목록(슬롯 순서 정렬)
    private List<AlbumDetailPhotoDto> photos;

    // [응답] 태그 문자열 목록
    private List<String> tags;

    // [응답] 앨범에 달린 달개(이모지 반응) 집계 목록
    private List<AlbumDalgaeDto> badges;

    // [응답] 현재 사용자가 이 앨범에 남긴 달개 이모지 목록
    private List<String> myBadges;
}
