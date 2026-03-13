package com.example.backend.badge.service;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.album.entity.AlbumEntity;
import com.example.backend.album.repository.AlbumRepository;
import com.example.backend.badge.dto.AlbumDalgaeDto;
import com.example.backend.badge.dto.AlbumDalgaeResponseDto;
import com.example.backend.badge.dto.BadgeCountMappingDto;
import com.example.backend.badge.dto.BadgeRankingDto;
import com.example.backend.badge.dto.BadgeStatsDto;
import com.example.backend.badge.dto.BadgeTypeDto;
import com.example.backend.badge.entity.Badge;
import com.example.backend.badge.entity.BadgeType;
import com.example.backend.badge.repository.BadgeRepository;
import com.example.backend.badge.repository.BadgeTypeRepository;
import com.example.backend.friend.repository.FriendshipRepository;
import com.example.backend.notification.service.NotificationService;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * [달개 서비스 (Badge Service)]
 * - 달개(좋아요) 통계 집계, 랭킹 계산, 달개 유형 조회
 *
 * [필요한 주입 객체]
 * 1. BadgeRepository badgeRepository : 달개 데이터 CRUD/집계
 * 2. BadgeTypeRepository badgeTypeRepository : 달개 유형 마스터 조회
 * 3. FriendshipRepository friendshipRepository : (선택) 친구 랭킹 시 친구 목록 필요
 * 4. UserRepository userRepository : 사용자 정보 조회
 *
 * [구현해야 할 메서드]
 *
 * 1. getStats(Long userId) → BadgeStatsDto
 * - 해당 사용자의 게시글들이 받은 달개를 유형별로 집계
 * - 쿼리 예시 (JPQL):
 * SELECT b.badgeType.name, COUNT(b)
 * FROM Badge b
 * WHERE b.post.user.id = :userId
 * GROUP BY b.badgeType.name
 * - 결과를 BadgeStatsDto로 변환 (유형별 개수 + 총합)
 *
 * 2. getGlobalRanking(String sortBy, int limit) → List<BadgeRankingDto>
 * - 전체 사용자의 받은 달개 수 기준 랭킹
 * - 쿼리 예시:
 * SELECT b.post.user, COUNT(b) as cnt
 * FROM Badge b
 * GROUP BY b.post.user
 * ORDER BY cnt DESC
 * - limit로 상위 N명만 반환
 * - sortBy가 특정 달개유형이면 해당 유형만 필터링
 *
 * 3. getFriendsRanking(Long userId, String sortBy, int limit) →
 * List<BadgeRankingDto>
 * - 현재 사용자의 친구들만 대상으로 랭킹
 * - 먼저 친구 목록 조회 → 해당 유저들의 달개 집계
 *
 * 4. getAllTypes() → List<BadgeTypeDto>
 * - BadgeType 전체 조회 → DTO 변환
 * - badgeTypeRepository.findAllByOrderBySortOrderAsc()
 *
 * [사용 어노테이션]
 * - @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {
    // 리포지토리 주입
    private final BadgeRepository badgeRepository;
    private final BadgeTypeRepository badgeTypeRepository;
    private final FriendshipRepository friendShipRepository;
    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // public BadgeStatsDto getStats(Long userId) {
    // // 여기에 달개 통계 조회 로직을 작성하세요.
    @Transactional(readOnly = true)
    public BadgeStatsDto getBadgeStats(Long userId) {
        // 1. 리포지토리 쿼리 연계 실행(내것/친구것 공용)
        List<BadgeCountMappingDto> results = badgeRepository.countByUserIdGroupByTypeId(userId);
        // 2. 조회를 위한 Map 변환(key:typeId,value:count)
        Map<Long, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        BadgeCountMappingDto::getTypeId,
                        BadgeCountMappingDto::getCount));
        // 3. 모든 배지 종류 가져오기
        List<BadgeType> allBadgeTypes = badgeTypeRepository.findAll();
        // 4. 상세 리스트 생성(0개의 달개 리스트도 포함하기하기 위해 포함)
        List<BadgeStatsDto.TypeCountDto> badgeTypeCount = allBadgeTypes.stream()
                .map(type -> BadgeStatsDto.TypeCountDto.builder()
                        .typeName(type.getName())
                        .emoji(type.getEmoji())
                        .count(countMap.getOrDefault(type.getId(), 0L).intValue())
                        .build())
                .collect(Collectors.toList());
        // 5. 전체 개수 합산
        int totalBadgeCount = badgeTypeCount.stream()
                .mapToInt(BadgeStatsDto.TypeCountDto::getCount)
                .sum();
        // 6. 결과 반환
        return BadgeStatsDto.builder()
                .totalCount(totalBadgeCount)
                .typeCounts(badgeTypeCount)
                .build();
    }//////

    // public List<BadgeRankingDto> getGlobalRanking(String sortBy, int limit) {
    // // 여기에 전체 랭킹 집계 로직을 작성하세요.
    @Transactional(readOnly = true)
    public Page<BadgeRankingDto> getAllRanking(
    		LocalDateTime startDate,LocalDateTime endDate, Pageable pageable) {
        // 1. 리포지토리의 페이징 처리된 데이터 가져오기
    	Pageable pageWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    	Page<BadgeRankingDto> rankings;
    	// 2. 날짜 조건에 따른 분기
    	if(startDate !=null && endDate !=null) {
    		//weekly
    		rankings = badgeRepository.findByAllBadgeRankingsWithDate(startDate,endDate,pageWithoutSort); 
    	} else  {
    		// alltime
    		rankings = badgeRepository.findByAllBadgeRankings(pageWithoutSort);
    		
    	}
    	
        // 2. 시작 순위 계산
    	int startRank = (int)pageable.getOffset() + 1;
    	
    	// 3. 인덱스를 활용한 rank 주입 및 변환
    	List<BadgeRankingDto> rankedContent = IntStream.range(0, rankings.getContent().size())
    	        .mapToObj(i -> {
    	            BadgeRankingDto dto = rankings.getContent().get(i);
        		return BadgeRankingDto.builder()
                    .rank(startRank + i) 
                    .userId(dto.getUserId())
                    .username(dto.getUsername())
                    .profileImageUrl(dto.getProfileImageUrl())
                    .totalBadges(dto.getTotalBadges())
                    .build();
            })
        	.collect(Collectors.toList());
    	
    	return new PageImpl<>(rankedContent,pageable,rankings.getTotalElements());
    }////

    // public List<BadgeRankingDto> getFriendsRanking(Long userId, String sortBy,
    // int limit) {
    // // 여기에 친구 랭킹 집계 로직을 작성하세요.
    @Transactional(readOnly = true)
    public Page<BadgeRankingDto> getFriendRanking(Long myId,Pageable pageable) {
    	// 1. 리포지토리의 페이징 처리된 데이터 가져오기
    	Pageable pageWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    	Page<BadgeRankingDto> rankings = badgeRepository.findByFriendsBadgeRankings(myId, pageWithoutSort);
    	// 2. 시작 순위 계산
    	int startRank = (int)pageable.getOffset() +1;
    	// 3. 인덱스를 활용한 rank 주입 및 변환
    	List<BadgeRankingDto> rankedContent = IntStream.range(0, rankings.getContent().size())
    	        .mapToObj(i -> {
    	            BadgeRankingDto dto = rankings.getContent().get(i);
        		return BadgeRankingDto.builder()
                    .rank(startRank + i) // offset + 현재 인덱스 +1
                    .userId(dto.getUserId())
                    .username(dto.getUsername())
                    .profileImageUrl(dto.getProfileImageUrl())
                    .totalBadges(dto.getTotalBadges())
                    .build();
            })
        	.collect(Collectors.toList());
    	
    	return new PageImpl<>(rankedContent,pageable,rankings.getTotalElements());
    }

    // public List<BadgeTypeDto> getAllTypes() {
    // // 여기에 달개 유형 전체 조회 로직을 작성하세요.
	public List<BadgeTypeDto> getBadgeTypes() {
		return badgeTypeRepository.findAllByOrderBySortOrderAsc();
	}



    // 전체 글에 부여된 달개 통계 (글로벌)
    @Transactional(readOnly = true)
    public BadgeStatsDto getGlobalBadgeStats() {
        List<BadgeCountMappingDto> results = badgeRepository.countAllGroupByTypeId();
        Map<Long, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        BadgeCountMappingDto::getTypeId,
                        BadgeCountMappingDto::getCount));
        List<BadgeType> allBadgeTypes = badgeTypeRepository.findAll();
        List<BadgeStatsDto.TypeCountDto> badgeTypeCount = allBadgeTypes.stream()
                .map(type -> BadgeStatsDto.TypeCountDto.builder()
                        .typeName(type.getName())
                        .emoji(type.getEmoji())
                        .count(countMap.getOrDefault(type.getId(), 0L).intValue())
                        .build())
                .collect(Collectors.toList());
        int totalBadgeCount = badgeTypeCount.stream()
                .mapToInt(BadgeStatsDto.TypeCountDto::getCount)
                .sum();
        return BadgeStatsDto.builder()
                .totalCount(totalBadgeCount)
                .typeCounts(badgeTypeCount)
                .build();
    }

    // ── 앨범 달개 부여/취소 (토글) ──

    /**
     * 앨범에 달개를 부여하거나, 이미 같은 달개가 있으면 취소(삭제)한다.
     * @param userId      달개를 부여하는 사용자 ID
     * @param albumId     대상 앨범 ID
     * @param badgeTypeId 달개 유형 ID
     * @return 토글 결과 (ADDED/REMOVED + 갱신된 달개 목록)
     */
    @Transactional
    public AlbumDalgaeResponseDto toggleAlbumDalgae(Long userId, Long albumId, Long badgeTypeId) {
        // 1. 앨범, 사용자, 달개유형 존재 확인
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("앨범을 찾을 수 없습니다."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        BadgeType badgeType = badgeTypeRepository.findById(badgeTypeId)
                .orElseThrow(() -> new IllegalArgumentException("달개 유형을 찾을 수 없습니다."));

        // 2. 이 유저가 이 앨범에 이미 남긴 달개가 있는지 확인 (1인 1달개 제한)
        java.util.Optional<Badge> existing = badgeRepository.findByUser_IdAndAlbum_Id(userId, albumId);

        String action;
        if (existing.isPresent()) {
            Badge prev = existing.get();
            if (prev.getBadgeType().getId().equals(badgeTypeId)) {
                // 같은 달개 → 취소 (토글 OFF)
                badgeRepository.delete(prev);
                badgeRepository.flush();
                action = "REMOVED";
            } else {
                // 다른 달개 → 기존 삭제 후 새 달개로 교체
                badgeRepository.delete(prev);
                badgeRepository.flush();
                Badge badge = Badge.builder()
                        .user(user)
                        .album(album)
                        .badgeType(badgeType)
                        .build();
                badgeRepository.save(badge);
                action = "CHANGED";
            }
        } else {
            // 없으면 새로 생성 (토글 ON)
            Badge badge = Badge.builder()
                    .user(user)
                    .album(album)
                    .badgeType(badgeType)
                    .build();
            badgeRepository.save(badge);

            // 앨범 작성자에게 알림 전송 (자기 자신에게는 알림 안 보냄)
            if (album.getUser() != null && !album.getUser().getId().equals(userId)) {
                notificationService.createNotification(
                        album.getUser().getId(),
                        "BADGE",
                        "새로운 달개",
                        user.getUsername() + "님이 달개(" + badgeType.getEmoji() + ")를 남겼습니다!"
                );
            }
            action = "ADDED";
        }

        // 3. 갱신된 달개 집계 + 내 달개 목록 반환
        List<AlbumDalgaeDto> badges = getAlbumDalgaeList(albumId);
        List<String> myBadges = badgeRepository.findMyBadgeEmojisByAlbumAndUser(albumId, userId);

        return AlbumDalgaeResponseDto.builder()
                .action(action)
                .badges(badges)
                .myBadges(myBadges)
                .build();
    }

    /**
     * 특정 앨범의 달개 집계 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<AlbumDalgaeDto> getAlbumDalgaeList(Long albumId) {
        List<Object[]> rows = badgeRepository.countByAlbumGroupByType(albumId);
        return rows.stream()
                .map(row -> AlbumDalgaeDto.builder()
                        .emoji((String) row[0])
                        .name((String) row[1])
                        .count((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 앨범에서 특정 사용자가 남긴 달개 이모지 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<String> getMyBadgesForAlbum(Long albumId, Long userId) {
        return badgeRepository.findMyBadgeEmojisByAlbumAndUser(albumId, userId);
    }

}
