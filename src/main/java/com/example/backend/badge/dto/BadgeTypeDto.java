package com.example.backend.badge.dto;

import lombok.*;

/**
 * [달개 유형 DTO (Badge Type)]
 * - 프론트엔드 badgeService.getAllTypes() 응답
 * - 달개 종류 마스터 정보를 프론트엔드에 전달
 *
 * [필요한 변수]
 * 1. id (Long) : 달개 유형 ID
 * 2. name (String) : 유형 이름 (예: "좋아요", "슬퍼요")
 * 3. emoji (String) : 이모지 (예: "❤️", "😢")
 * 4. sortOrder (int) : 표시 순서
 *
 * [프론트엔드 사용 위치]
 * - BadgesPage, PostDetailPage 에서 달개 유형 선택 UI 렌더링
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeTypeDto {
    private Long id;
    private String name;
    private String emoji;
    private int sortOrder;
    private String description;
}
