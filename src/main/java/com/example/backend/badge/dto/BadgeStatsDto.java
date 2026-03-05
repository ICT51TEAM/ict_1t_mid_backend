package com.example.backend.badge.dto;

import lombok.*;

import java.util.List;

/**
 * [달개 통계 DTO (Badge Stats)]
 * - 프론트엔드 badgeService.getMyStats() / getUserStats() 응답
 * - 특정 사용자가 받은 달개의 유형별 통계
 *
 * [필요한 변수]
 * 1. totalCount (int) : 받은 달개 총 개수
 * 2. typeCounts (List<TypeCountDto>) : 유형별 개수 목록
 *
 * [TypeCountDto 내부 변수]
 * - typeName (String) : 달개 유형 이름 (예: "좋아요")
 * - emoji (String) : 이모지 (예: "❤️")
 * - count (int) : 해당 유형의 개수
 *
 * [변환 예시]
 * {
 * "totalCount": 42,
 * "typeCounts": [
 * { "typeName": "좋아요", "emoji": "❤️", "count": 25 },
 * { "typeName": "슬퍼요", "emoji": "😢", "count": 8 },
 * { "typeName": "화나요", "emoji": "😠", "count": 3 },
 * { "typeName": "응원해요", "emoji": "💪", "count": 6 }
 * ]
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeStatsDto {
    private int totalCount;
    private List<TypeCountDto> typeCounts;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeCountDto {
        private String typeName;
        private String emoji;
        private int count;
    }
    
    public interface BadgeCountProjection {
        Long getTypeId();
        Long getCount();
    }
}
