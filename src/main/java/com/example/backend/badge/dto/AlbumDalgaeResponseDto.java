package com.example.backend.badge.dto;

import java.util.List;

import lombok.*;

/**
 * [앨범 달개 토글 응답 DTO]
 * - 달개 부여/취소 후 프론트엔드에 반환하는 응답
 * - action: "ADDED" 또는 "REMOVED"
 * - badges: 해당 앨범의 전체 달개 집계 목록
 * - myBadges: 내가 이 앨범에 남긴 달개 이모지 목록
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDalgaeResponseDto {
    private String action;
    private List<AlbumDalgaeDto> badges;
    private List<String> myBadges;
}
