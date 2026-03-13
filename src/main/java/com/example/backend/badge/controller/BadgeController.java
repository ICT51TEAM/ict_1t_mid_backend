package com.example.backend.badge.controller;

import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;

import com.example.backend.badge.dto.AlbumDalgaeResponseDto;
import com.example.backend.badge.dto.BadgeRankingDto;
import com.example.backend.badge.dto.BadgeStatsDto;
import com.example.backend.badge.dto.BadgeTypeDto;
import com.example.backend.badge.service.BadgeService;
import com.example.backend.global.config.JwtUtil;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * [달개(뱃지) 컨트롤러 (Badge Controller)]
 * - 프론트엔드 badgeService.js의 /api/badges/* 요청을 처리합니다.
 * - 달개 통계, 랭킹, 달개 유형 조회
 *
 * [프론트엔드 badgeService.js와의 매핑]
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ 프론트 함수 │ 백엔드 엔드포인트 │
 * ├──────────────────────────────────┼────────────────────────────────┤
 * │ badgeService.getMyStats() │ GET /api/badges/stats │
 * │ badgeService.getUserStats(id) │ GET /api/badges/stats/{userId} │
 * │ badgeService.getGlobalRanking() │ GET /api/badges/ranking/global │
 * │ badgeService.getFriendsRanking() │ GET /api/badges/ranking/friends│
 * │ badgeService.getAllTypes() │ GET /api/badges/types │
 * │ badgeService.getLatestFriendStory()│ GET /api/posts/latest-friend │
 * └──────────────────────────────────┴────────────────────────────────┘
 *
 * [필요한 주입 서비스]
 * - BadgeService badgeService
 */
@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController implements BadgeControllerDocs {
    //서비스 주입
    private final BadgeService badgeService;
    private final JwtUtil jwtUtil;
    
	

    /**
     * [1] 내 달개 통계 — GET /api/badges/stats
     * 
     * @return BadgeStatsDto (각 달개 유형별 받은 개수)
     *
     *         로직 힌트:
     *         - 현재 로그인 사용자의 ID로 달개 통계 조회
     *         - badgeService.getStats(userId)
     */
    @Operation(summary = "나의 달개 통계 조회", description = "내가 부여받은 달개의 총 개수와 달개 종류별 개수를 조회합니다")
	@GetMapping("/stats")
    public ResponseEntity<BadgeStatsDto> getMyBadgeStats(Authentication authentication) {
    	// 필터에서 저장한 내 userId 추출
    	Long myId = Long.valueOf(authentication.getName());
    	// 서비스 호출
	    BadgeStatsDto stats = badgeService.getBadgeStats(myId);
	    // 결과값 반환
	    return ResponseEntity.ok(stats);

    }////

    /**
     * [2] 특정 사용자 달개 통계 — GET /api/badges/stats/{userId}
     * 
     * @param userId 조회할 사용자 ID
     * @return BadgeStatsDto
     */
    // @GetMapping("/stats/{userId}")
    // public ResponseEntity<?> getUserStats(@PathVariable Long userId) {
    // // 여기에 코드를 작성하세요.
    @Operation(summary = "친구의 달개 통계 조회", description = "친구가 부여받은 달개의 총 개수와 달개 종류별 개수를 조회합니다")
	@GetMapping("/stats/{userId}")
    public ResponseEntity<BadgeStatsDto> getFriendsBadgeStats(@PathVariable Long userId) {
    	// 경로 변수에서 내 친구의 userId 그대로 받아서 서비스 호출 및 사용
	    BadgeStatsDto stats = badgeService.getBadgeStats(userId);
	    // 결과값 반환
	    return ResponseEntity.ok(stats);
    }////

    /**
     * [3] 전체 사용자 랭킹 — GET /api/badges/ranking/global
     * 
     * @param sortBy 정렬 기준 (쿼리 파라미터, 예: "total", "좋아요")
     * @param limit  조회 수 제한 (쿼리 파라미터)
     * @return List<BadgeRankingDto>
     *
     *         로직 힌트:
     *         - 모든 사용자의 받은 달개 총합 집계 → 내림차순 정렬
     *         - GROUP BY user → COUNT(*) 또는 SUM
     *         - JPQL: SELECT new BadgeRankingDto(b.post.user, COUNT(b)) FROM Badge
     *         b GROUP BY b.post.user ORDER BY COUNT(b) DESC
     */
    // @GetMapping("/ranking/global")
    // public ResponseEntity<?> getGlobalRanking(
    // @RequestParam(required = false) String sortBy,
    // @RequestParam(required = false, defaultValue = "10") int limit) {
    
    @GetMapping("/ranking/global")
    public ResponseEntity<Page<BadgeRankingDto>> getGlobalRanking(
    		@PageableDefault(page = 0, size = 10,sort = "id", direction = Sort.Direction.DESC) Pageable pagable,
    		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
    		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate) {
    	
    	LocalDateTime startDateTime = (startDate !=null) ? startDate.atStartOfDay() : null; 
    	LocalDateTime endDateTime = (endDate !=null) ? endDate.atTime(23,59,59) : null; 
    	Page<BadgeRankingDto> ranking = badgeService.getAllRanking(startDateTime,endDateTime,pagable);
    	return ResponseEntity.ok(ranking);
    }//////

    /**
     * [4] 친구 랭킹 — GET /api/badges/ranking/friends
     * 
     * @return List<BadgeRankingDto>
     *
     *         로직 힌트:
     *         - 현재 사용자의 친구 목록 조회 → 친구들의 달개 통계만 집계
     *         - FriendService에서 친구 User 목록 조회 후 필터링
     */
    // @GetMapping("/ranking/friends")
    // public ResponseEntity<?> getFriendsRanking(
    // @RequestParam(required = false) String sortBy,
    // @RequestParam(required = false, defaultValue = "10") int limit) {
    
    @GetMapping("/ranking/friends")
    public ResponseEntity<Page<BadgeRankingDto>> getFriendsRanking(
    		@AuthenticationPrincipal Long myId,
    		@PageableDefault(page = 0, size = 10,sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    	Page<BadgeRankingDto>ranking = badgeService.getFriendRanking(myId,pageable);
    	return ResponseEntity.ok(ranking);
    }//////

    /**
     * [5-1] 전체 글 달개 통계 — GET /api/badges/stats/global
     */
    @Operation(summary = "전체 달개 통계 조회", description = "모든 글에 부여된 달개의 총 개수와 종류별 개수를 조회합니다")
    @GetMapping("/stats/global")
    public ResponseEntity<BadgeStatsDto> getGlobalBadgeStats() {
        BadgeStatsDto stats = badgeService.getGlobalBadgeStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * [5] 전체 달개 유형 목록 — GET /api/badges/types
     * 
     * @return List<BadgeTypeDto>
     *
     *         로직 힌트:
     *         - BadgeTypeRepository.findAllByOrderBySortOrderAsc() 호출
     *         - BadgeType → BadgeTypeDto 변환
     */
    // @GetMapping("/types")
    // public ResponseEntity<?> getAllTypes() {
    @Operation(summary = "전제 달개 유형 목록", description = "설정되어 있는 모든 달개의 유형들에 대한 리스트입니다")
	@GetMapping("/types")
    public ResponseEntity<List<BadgeTypeDto>> getAllBadgeTypes() {
    	// 서비스 호출
    	List<BadgeTypeDto> types = badgeService.getBadgeTypes();
	    // 결과값 반환
	    return ResponseEntity.ok(types);

    }////

    /**
     * [6] 앨범 달개 토글 — POST /api/badges/albums/{albumId}/toggle
     *
     * 달개를 부여하거나, 이미 같은 달개가 있으면 취소(삭제)한다.
     *
     * @param albumId     대상 앨범 ID (PathVariable)
     * @param badgeTypeId 달개 유형 ID (RequestParam)
     * @param authentication 현재 로그인 사용자
     * @return AlbumDalgaeResponseDto (action + 갱신된 달개 목록 + 내 달개 목록)
     */
    @Operation(summary = "앨범 달개 토글", description = "앨범에 달개를 부여하거나 취소합니다 (토글)")
    @PostMapping("/albums/{albumId}/toggle")
    public ResponseEntity<?> toggleAlbumDalgae(
            @PathVariable Long albumId,
            @RequestParam Long badgeTypeId,
            Authentication authentication) {
        try {
            Long userId = Long.valueOf(authentication.getName());
            AlbumDalgaeResponseDto result = badgeService.toggleAlbumDalgae(userId, albumId, badgeTypeId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * [7] 앨범 달개 조회 — GET /api/badges/albums/{albumId}
     *
     * 특정 앨범의 달개 집계 + 내가 남긴 달개 목록을 조회한다.
     */
    @Operation(summary = "앨범 달개 조회", description = "앨범에 달린 달개 목록과 내가 남긴 달개를 조회합니다")
    @GetMapping("/albums/{albumId}")
    public ResponseEntity<?> getAlbumDalgae(
            @PathVariable Long albumId,
            Authentication authentication) {
        Long userId = (authentication != null) ? Long.valueOf(authentication.getName()) : null;
        var badges = badgeService.getAlbumDalgaeList(albumId);
        var myBadges = (userId != null) ? badgeService.getMyBadgesForAlbum(albumId, userId) : java.util.List.of();
        return ResponseEntity.ok(Map.of("badges", badges, "myBadges", myBadges));
    }

}
