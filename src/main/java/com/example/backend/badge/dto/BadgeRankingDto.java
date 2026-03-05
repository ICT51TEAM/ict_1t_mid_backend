package com.example.backend.badge.dto;

import lombok.*;

/**
 * [달개 랭킹 DTO (Badge Ranking)]
 * - 프론트엔드 badgeService.getGlobalRanking() / getFriendsRanking() 응답
 * - 사용자별 받은 달개 수 기준 랭킹 정보
 *
 * [필요한 변수]
 * 1. rank (int) : 순위 (1, 2, 3, ...)
 * 2. userId (Long) : 사용자 ID
 * 3. username (String) : 닉네임
 * 4. profileImageUrl (String) : 프로필 이미지 URL
 * 5. totalBadges (int) : 받은 달개 총 개수
 *
 * [프론트엔드 사용 위치]
 * - BadgeRankingPage에서 랭킹 테이블/리스트 렌더링
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeRankingDto {
    private int rank;
    private Long userId;
    private String username;
    private String profileImageUrl;
    private long totalBadges;
}
