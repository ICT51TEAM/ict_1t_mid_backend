package com.example.backend.friend.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.friend.dto.FriendRequestDto;
import com.example.backend.friend.dto.FriendResponseDto;
import com.example.backend.friend.dto.UserSearchDto;

@Tag(name = "친구 API", description = "친구 조회, 요청, 수락, 거절 등 친구 관련 기능을 제공하는 API입니다.")
public interface FriendControllerDocs {

    @Operation(summary = "내 친구 목록 조회", description = "현재 로그인한 사용자의 수락 완료된 친구 목록을 반환합니다.")
    ResponseEntity<List<FriendResponseDto>> getAcceptedFriends(Authentication authentication);

    @Operation(summary = "받은 친구 요청 목록 조회", description = "대기 중인(수락하지 않은) 친구 요청 목록을 반환합니다.")
    ResponseEntity<List<FriendResponseDto>> getPendingRequests(Authentication authentication);

    @Operation(summary = "상대방에게 친구요청 발송", description = "특정 사용자에게 친구 요청을 보냅니다.")
    ResponseEntity<String> sendFriendRequest(Authentication authentication, @RequestBody FriendRequestDto requestDto);

    @Operation(summary = "친구 요청 수락 처리", description = "받은 친구 요청을 수락합니다.")
    ResponseEntity<String> acceptFriendRequest(Authentication authentication, @PathVariable Long friendshipId);

    @Operation(summary = "친구 요청 거절 처리", description = "받은 친구 요청을 거절 상태로 변경합니다.")
    ResponseEntity<String> rejectFriendRequest(Authentication authentication, @PathVariable Long friendshipId);

    @Operation(summary = "등록된 친구 삭제", description = "이미 등록된 친구를 친구 목록에서 삭제합니다.")
    ResponseEntity<String> deleteFriend(@PathVariable Long friendId, Authentication authentication);

    @Operation(summary = "유저 닉네임 검색", description = "특정 키워드(닉네임)를 포함하는 유저 목록을 검색합니다.")
    ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam("q") String query, Authentication authentication);
}