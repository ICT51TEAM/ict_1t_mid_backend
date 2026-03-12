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
    private Long getUserIdFromToken(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        try {
            // 현재 보안 컨텍스트에 저장된 Principal(251 등)을 Long으로 변환
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (Exception e) {
            return null;
        }
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
    		Authentication authentication) {
    	Long userId = getUserIdFromToken(authentication);
        return ResponseEntity.ok(friendService.listPendingRequests(userId));
    }

    // 3. 상대방에게 친구요청 발송
    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(
    		Authentication authentication,
            @RequestBody FriendRequestDto requestDto) {
    	Long userId = getUserIdFromToken(authentication);
    	
        try {
            friendService.sendRequest(userId, requestDto.getTargetUserId());
            return ResponseEntity.ok("성공");
        } catch (IllegalArgumentException e) {
            // 500 대신 400(Bad Request)을 반환하여 로직상 거부됨을 명시
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 내가 보낸 친구 요청 중 아직 수락 대기 중인 목록 조회(추가)
    @GetMapping("/pending/sent")
    public ResponseEntity<List<FriendResponseDto>> getSentPendingRequests(
    		Authentication authentication) {
    	// 1. 인증 객체가 아예 없는 경우 방어 코드
        if (authentication == null || authentication.getPrincipal() == null) {
            System.out.println("인증 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 2. 필터에서 넘겨준 Long 타입의 userId를 꺼냅니다.
            // (만약 여기서 에러가 난다면 (String)으로 변환 후 Long.valueOf()를 써야 할 수도 있습니다)
            Long userId = (Long) authentication.getPrincipal();
            
            System.out.printf("데이터 조회 시작 - 유효한 유저 ID: {}", userId);

            // 3. 서비스 호출 (이제 userId가 null이 아니므로 500 에러가 발생하지 않습니다)
            List<FriendResponseDto> sentList = friendService.listSentPendingRequests(userId);
            return ResponseEntity.ok(sentList);
            
        } catch (Exception e) {
            System.out.printf("컨트롤러 로직 에러: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 4. 특정된 친구 요청 수락 처리
    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<String> acceptFriendRequest(
    		Authentication authentication,
            @PathVariable Long friendshipId) {
    	Long userId = getUserIdFromToken(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        friendService.acceptRequest(friendshipId, userId);
        return ResponseEntity.ok("친구 요청을 수락했습니다");
    }

    // 5. 특정된 친구 요청 거절 처리
    @PostMapping("/{friendshipId}/reject")
    public ResponseEntity<String> rejectFriendRequest(
    		Authentication authentication,
            @PathVariable Long friendshipId) {
    	Long userId = getUserIdFromToken(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        friendService.rejectRequest(friendshipId, userId);
        return ResponseEntity.ok("친구 요청을 거절했습니다");
    }

    // 6. DELETE /api/friends/{friendId} : 이미 등록된 친구 삭제
    @DeleteMapping("/{friendId}")
    public ResponseEntity<String> deleteFriend(
            @PathVariable Long friendId,
            Authentication authentication) {
    	Long userId = getUserIdFromToken(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        friendService.removeFriend(friendId, userId);
        return ResponseEntity.ok("친구 관계를 삭제했습니다");
    }

    // 7. GET /api/friends/search?q={검색어} : 특정 닉네임 유저 검색
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(
            @RequestParam("q") String query,
            Authentication authentication) {
        
        Long userId = getUserIdFromToken(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<UserSearchDto> userList = friendService.searchUsers(query, userId);
        return ResponseEntity.ok(userList);
    }

}
