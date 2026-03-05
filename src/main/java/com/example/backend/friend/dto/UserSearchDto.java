package com.example.backend.friend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * [사용자 검색 결과 DTO (User Search)]
 * - 프론트엔드 friendService.searchUsers(query) 응답으로 반환
 * - GET /api/friends/search?q=검색어 의 응답
 *
 * [필요한 변수]
 * 1. userId (Long) : 검색된 사용자 ID
 * 2. username (String) : 닉네임
 * 3. profileImageUrl (String) : 프로필 이미지 URL
 * 4. isFriend (boolean) : 이미 친구인지 여부 (프론트에서 "친구 추가" 버튼 표시 제어)
 * 5. isPending (boolean) : 이미 친구 요청 중인지 여부
 *
 * [프론트엔드 사용 위치]
 * - AddFriendPage에서 사용자 검색 결과 목록 렌더링
 * - isFriend=true이면 "이미 친구입니다" 표시
 * - isPending=true이면 "요청 보냄" 표시
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchDto {
	 private Long userId;
	    private String username;
	    private String profileImageUrl;
	    @JsonProperty("isFriend")
	    private boolean isFriend;
	    @JsonProperty("isPending")
	    private boolean isPending;
	    private String representativeBadge;
	    private int badgeCount;
}
