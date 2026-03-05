package com.example.backend.friend.dto;

import lombok.*;

/**
 * [친구 정보 응답 DTO (Friend Response)]
 * - 친구 목록 / 받은 요청 목록 조회 시 프론트엔드로 반환하는 데이터 구조
 *
 * [필요한 변수]
 * 1. friendshipId (Long) : Friendship 엔티티의 PK (수락/거절 시 이 ID 사용)
 * 2. userId (Long) : 상대방 사용자 ID
 * 3. username (String) : 상대방 닉네임
 * 4. profileImageUrl (String) : 상대방 프로필 이미지 URL
 * 6. status (String) : 관계 상태 ("PENDING" / "ACCEPTED")
 *
 * [프론트엔드 사용 위치]
 * - friendService.listFriends() → FriendsPage에서 친구 목록 렌더링
 * - friendService.listPendingRequests() → FriendsPage에서 받은 요청 렌더링
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendResponseDto {
    private Long friendshipId;
    private Long userId;
    private String username;
    private String profileImageUrl;
    private String status;
}
