package com.example.backend.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [친구 요청 DTO (Friend Request)]
 * - 프론트엔드 friendService.sendRequest(targetUserId) 에서 전송
 * - POST /api/friends/request 의 @RequestBody
 *
 * [필요한 변수]
 * 1. targetUserId (Long) : 친구 요청을 보낼 대상 사용자의 ID
 *
 * [비즈니스 로직 힌트]
 * - 현재 로그인 사용자 = fromUser (SecurityContext에서 추출)
 * - targetUserId = toUser
 * - 중복 요청 검사: 이미 관계가 존재하는지 FriendshipRepository.findByUsers() 확인
 * - 자기 자신에게 요청 방지
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDto {
    private Long targetUserId;
}
