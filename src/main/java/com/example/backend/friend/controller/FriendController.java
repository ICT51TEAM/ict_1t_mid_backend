package com.example.backend.friend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import com.example.backend.friend.dto.FriendRequestDto;
import com.example.backend.friend.dto.FriendResponseDto;
import com.example.backend.friend.service.FriendService;
import com.example.backend.friend.dto.UserSearchDto;
import com.example.backend.global.config.JwtUtil;
import com.example.backend.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController implements FriendControllerDocs {
    private final FriendService friendService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // JWT 토큰에서 userId 추출
    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("인증 토큰이 없습니다.");
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    // JWT 토큰에서 email 추출
    private String getEmailFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("인증 토큰이 없습니다.");
        }
        String token = authHeader.substring(7);
        return jwtUtil.getUserEmailFromToken(token);
    }

    // 1. 내 친구 목록 조회
    @GetMapping
    public ResponseEntity<List<FriendResponseDto>> getAcceptedFriends(
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long myId = Long.valueOf(authentication.getName());
        List<FriendResponseDto> friendList = friendService
                .getAcceptedFriendsList(myId);

        return ResponseEntity.ok().body(friendList);
    }

    // 2. 받은 친구 요청 목록 조회
    @GetMapping("/pending")
    public ResponseEntity<List<FriendResponseDto>> getPendingRequests(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(friendService.listPendingRequests(userId));
    }

    // 3. 상대방에게 친구요청 발송
    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(
            @AuthenticationPrincipal Long userId,
            @RequestBody FriendRequestDto requestDto) {
        friendService.sendRequest(userId, requestDto.getTargetUserId());
        return ResponseEntity.ok("친구 요청을 성공적으로 발송했습니다.");
    }

    // 4. 특정된 친구 요청 수락 처리
    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<String> acceptFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendshipId) {
        friendService.acceptRequest(friendshipId, userId);
        return ResponseEntity.ok("친구 요청을 수락했습니다");
    }

    // 5. 특정된 친구 요청 거절 처리
    @PostMapping("/{friendshipId}/reject")
    public ResponseEntity<String> rejectFriendRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendshipId) {
        friendService.rejectRequest(friendshipId, userId);
        return ResponseEntity.ok("친구 요청을 거절했습니다");
    }

    // 6. DELETE /api/friends/{friendId} : 이미 등록된 친구 삭제
    @DeleteMapping("/{friendId}")
    public ResponseEntity<String> deleteFriend(
            @PathVariable Long friendId,
            @AuthenticationPrincipal Long userId) {
        friendService.removeFriend(friendId, userId);
        return ResponseEntity.ok("친구 관계를 삭제했습니다");
    }

    // 7. GET /api/friends/search?q={검색어} : 특정 닉네임 유저 검색
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(
            @RequestParam("q") String query,
            @AuthenticationPrincipal Long userId) {
        List<UserSearchDto> userList = friendService.searchUsers(query, userId);
        return ResponseEntity.ok(userList);
    }

}
