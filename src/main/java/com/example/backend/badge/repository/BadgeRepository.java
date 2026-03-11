package com.example.backend.badge.repository;

import com.example.backend.badge.dto.BadgeCountMappingDto;
import com.example.backend.badge.dto.BadgeRankingDto;
import com.example.backend.badge.dto.BadgeTypeDto;
import com.example.backend.badge.entity.Badge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [달개 레포지토리 (Badge Repository)] - Badge 엔티티에 대한 DB CRUD를 담당
 *
 * [구현해야 할 커스텀 쿼리 메서드]
 *
 * 1. 특정 게시글의 달개 수 조회 (유형별) @Query("SELECT b.badgeType.name, COUNT(b) FROM Badge
 * b WHERE b.post = :post GROUP BY b.badgeType.name") List<Object[]>
 * countByPostGroupByType(@Param("post") Post post);
 *
 * 2. 특정 사용자의 받은 달개 총 집계 (유형별) @Query("SELECT b.badgeType.name, COUNT(b) FROM
 * Badge b WHERE b.post.user = :user GROUP BY b.badgeType.name") List<Object[]>
 * countByPostUserGroupByType(@Param("user") User user);
 *
 * 3. 중복 달개 확인 (사용자가 같은 게시글에 같은 유형 달개를 이미 남겼는지) Optional<Badge>
 * findByUserAndPostAndBadgeType_Id(User user, Post post, Long badgeTypeId);
 *
 * 4. 전체 사용자 랭킹 집계 @Query("SELECT b.post.user.id, b.post.user.username, COUNT(b)
 * as cnt " + "FROM Badge b GROUP BY b.post.user.id, b.post.user.username ORDER
 * BY cnt DESC") List<Object[]> findGlobalRanking();
 */
public interface BadgeRepository extends JpaRepository<Badge, Long> {

	// @Query("SELECT b.badgeType.name, COUNT(b) FROM Badge b WHERE b.post.user =
	// :user GROUP BY b.badgeType.name")
	// List<Object[]> countByPostUserGroupByType(@Param("user") User user);

	// Optional<Badge> findByUserAndPostAndBadgeType_Id(User user, Post post, Long
	// badgeTypeId);

	// @Query("SELECT b.post.user.id, b.post.user.username, COUNT(b) as cnt FROM
	// Badge b GROUP BY b.post.user.id, b.post.user.username ORDER BY cnt DESC")
	// List<Object[]> findGlobalRanking();

	// 사용자(나 또는 친구 공용)의 달개 통계 산출물 — 내 앨범에 "받은" 달개를 집계
	@Query("SELECT new com.example.backend.badge.dto.BadgeCountMappingDto(b.badgeType.id, COUNT(b)) "
			+ "FROM Badge b WHERE b.album.user.id = :userId "
			+ "GROUP BY b.badgeType.id")
	List<BadgeCountMappingDto> countByUserIdGroupByTypeId(@Param("userId") Long userId);

	// 글로벌 기준 달개 랭킹 통계 산출물(alltime)
	@Query("SELECT new com.example.backend.badge.dto.BadgeRankingDto("
			+ "0, u.id, u.username, u.profileImageUrl, COUNT(b)) " + "FROM UserEntity u "
			+ "LEFT JOIN Badge b ON u.id = b.user.id " + "GROUP BY u.id, u.username, u.profileImageUrl "
			+ "ORDER BY COUNT(b) DESC, u.id ASC")
	Page<BadgeRankingDto> findByAllBadgeRankings(Pageable pageable);

	// 글로벌 기준 달개 랭킹 통계 산출물(weekly)
	@Query("SELECT new com.example.backend.badge.dto.BadgeRankingDto(" +
			"0, u.id, u.username, u.profileImageUrl, COUNT(b)) " +
			"FROM UserEntity u " +
			"LEFT JOIN Badge b ON u.id = b.user.id " +
			"AND b.createdAt BETWEEN :startDate AND :endDate " +
			"GROUP BY u.id, u.username, u.profileImageUrl " +
			"ORDER BY COUNT(b) DESC, u.id ASC")
	Page<BadgeRankingDto> findByAllBadgeRankingsWithDate(
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate,
			Pageable pageable);

	// 친구 기준 달개 랭킹 통계 산출물(alltime)
	@Query("SELECT new com.example.backend.badge.dto.BadgeRankingDto(0, u.id, u.username, u.profileImageUrl, COUNT(b)) "
			+ "FROM UserEntity u "
			+ "JOIN Badge b ON u.id = b.user.id "
			+ "JOIN Friendship f ON (u.id = f.fromUser.id OR u.id = f.toUser.id) "
			+ "WHERE (f.fromUser.id = :myId OR f.toUser.id = :myId) "
			+ "AND f.status = 'ACCEPTED' "
			+ "AND u.id != :myId " // :userId -> :myId 로 변경
			+ "GROUP BY u.id, u.username, u.profileImageUrl "
			+ "ORDER BY COUNT(b) DESC, u.id ASC")
	Page<BadgeRankingDto> findByFriendsBadgeRankings(@Param("myId") Long myId, Pageable pageable);

	/*
	 * // 달개 관련 통계 산출물 계산 // 1. 총 달개 수: 사용자의 앨범(Album)과 연결된 모든 달개(AlbumDalgae) 카운트
	 * 
	 * @Query("SELECT COUNT(ad) FROM AlbumDalgae ad WHERE ad.album.user.id = :userId"
	 * ) long countTotalDalgaeByUserId(@Param("userId") Long userId);
	 * 
	 * // 2. 달개 종류 수: 중복된 달개 타입을 제외하고 카운트
	 * 
	 * @Query("SELECT COUNT(DISTINCT ad.badgeType.id) FROM AlbumDalgae ad WHERE ad.album.user.id = :userId"
	 * ) long countDistinctBadgeTypesByUserId(@Param("userId") Long userId);
	 */

	// 유저와 관련된 모든 달개(앨범달개, 게시글달개) 일괄 삭제
	@Modifying
	@Query("DELETE FROM Badge b WHERE b.album.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);

	// 유저의 게시글에 달린 모든 달개 삭제 (다른 유저가 남긴 것 포함)
	@Modifying
	@Query("DELETE FROM Badge b WHERE b.album.user.id = :userId")
	void deleteByAlbumUserId(@Param("userId") Long userId);

	// ── 앨범 달개 부여/조회 관련 쿼리 ──

	// 특정 앨범에 특정 유저가 특정 타입의 달개를 이미 남겼는지 확인
	Optional<Badge> findByUser_IdAndAlbum_IdAndBadgeType_Id(Long userId, Long albumId, Long badgeTypeId);

	// 특정 앨범에 특정 유저가 남긴 달개 조회 (타입 무관, 1인 1달개 제한용)
	Optional<Badge> findByUser_IdAndAlbum_Id(Long userId, Long albumId);

	// 특정 앨범의 달개를 유형별로 집계 (emoji, name, count)
	@Query("SELECT bt.emoji AS emoji, bt.name AS name, COUNT(b) AS count " +
			"FROM Badge b JOIN b.badgeType bt " +
			"WHERE b.album.id = :albumId " +
			"GROUP BY bt.emoji, bt.name, bt.sortOrder " +
			"ORDER BY bt.sortOrder ASC")
	List<Object[]> countByAlbumGroupByType(@Param("albumId") Long albumId);

	// 특정 앨범에 내가 남긴 달개의 이모지를 조회
	@Query("SELECT bt.emoji FROM Badge b JOIN b.badgeType bt " +
			"WHERE b.album.id = :albumId AND b.user.id = :userId")
	List<String> findMyBadgeEmojisByAlbumAndUser(@Param("albumId") Long albumId, @Param("userId") Long userId);

	// 특정 앨범에 달린 모든 달개 삭제 (앨범 삭제 시 FK 정리)
	@Modifying
	@Query("DELETE FROM Badge b WHERE b.album.id = :albumId")
	void deleteByAlbumId(@Param("albumId") Long albumId);

	// 내가 남의 앨범에 남긴 달개 삭제 (탈퇴 시 FK 정리)
	@Modifying
	@Query("DELETE FROM Badge b WHERE b.user.id = :userId")
	void deleteByGivenUserId(@Param("userId") Long userId);
}
