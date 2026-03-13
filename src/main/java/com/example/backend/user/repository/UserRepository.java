package com.example.backend.user.repository;

import com.example.backend.user.dto.UserProfileDto;
import com.example.backend.user.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;


/**
 * [사용자 레포지토리 (User Repository)]
 * - User 엔티티에 대한 DB CRUD를 담당하는 Spring Data JPA 인터페이스
 *
 * [자동 제공되는 기본 메서드 (JpaRepository 상속)]
 * - save(User user) : 저장/수정
 * - findById(Long id) : PK로 조회
 * - findAll() : 전체 조회
 * - delete(User user) : 삭제
 * - existsById(Long id) : 존재 여부
 *
 * [구현해야 할 커스텀 쿼리 메서드]
 * 1. findByEmail(String email) → Optional<User>
 * - 로그인 시 이메일로 사용자 조회
 * - 카카오 로그인 시 기존 가입 여부 확인
 *
 * 2. existsByEmail(String email) → boolean
 * - 회원가입 시 이메일 중복 검사
 *
 * 3. findByUsername(String username) → Optional<User>
 * - 닉네임으로 사용자 검색 (선택 사항)
 *
 * 4. findByUsernameContaining(String keyword) → List<User>
 * - 닉네임 부분 검색 (친구 검색 기능에서 활용)
 * - SQL: WHERE username LIKE '%keyword%'
 *
 * [사용 방법]
 * - 메서드명만 선언하면 Spring Data JPA가 이름 규칙에 따라 자동으로 SQL 생성
 * - 복잡한 쿼리가 필요한 경우 @Query 어노테이션 사용:
 * @Query("SELECT u FROM User u WHERE u.email = :email AND u.provider =
 * :provider")
 * Optional<User> findByEmailAndProvider(@Param("email") String
 * email, @Param("provider") String provider);
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
	 // 1. findByEmail(String email) → Optional<User>
	 // - 로그인 시 이메일로 사용자 조회
	 // - 카카오 로그인 시 기존 가입 여부 확인
     Optional<UserEntity> findByEmail(String email);
     
     // [추가] 카카오 고유 ID로 사용자 조회
     // providerId는 카카오에서 넘겨주는 숫자 ID.
     Optional<UserEntity> findByOauthProviderId(String oauthProviderId);
     // 카카오 로그인시 추가 사항을 위한 메소드
     Optional<UserEntity> findByOauthProviderIdAndProvider(String oauthProviderId, String provider);
     
     
     // 2. existsByEmail(String email) → boolean
     // - 회원가입 시 이메일 중복 검사
    boolean existsByEmail(String email);
    //회원 검색후 UserProfileDto 반환
    @Query("""
			select new com.example.backend.user.dto.UserProfileDto(
				u.id, 
				u.email, 
				u.username, 
				u.profileImageUrl, 
				'', 
				u.provider, 
				u.visibility
			)
			from UserEntity u
			order by u.createdAt desc
			""")
    List<UserProfileDto> findUsersDto();
    // 3. findByUsername(String username) → Optional<User>
    // - 닉네임으로 사용자 검색 (선택 사항)
    Optional<UserEntity> findByUsername(String username);
    // 4. findByUsernameContaining(String keyword) → List<User>
    //  - 닉네임 부분 검색 (친구 검색 기능에서 활용)
    //  - SQL: WHERE username LIKE '%keyword%'
    List<UserEntity> findByUsernameContaining(String username);

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) AND (u.visibility IS NULL OR u.visibility <> 'PRIVATE')")
    List<UserEntity> searchByUsernameIgnoreCaseExcludePrivate(@Param("keyword") String keyword);

 // 유저의 설정 삭제
    @Modifying
    @Query("DELETE FROM UserSettingsEntity s WHERE s.user.id = :userId")
    void deleteSettingsByUserId(@Param("userId") Long userId);

    // ── 회원탈퇴용 삭제 메서드 (엔티티 없는 테이블 → nativeQuery 사용) ──

    // 앨범 달개 삭제 (유저 앨범에 달린 달개 + 유저가 남긴 달개)
    @Modifying
    @Query(value = "DELETE FROM ALBUM_DALGAE WHERE GIVEN_BY_USER_ID = :userId OR ALBUM_ID IN (SELECT ALBUM_ID FROM ALBUM WHERE USER_ID = :userId)", nativeQuery = true)
    void deleteAlbumDalgaeByUserId(@Param("userId") Long userId);

    // 앨범 사진 삭제
    @Modifying
    @Query(value = "DELETE FROM ALBUM_PHOTO WHERE ALBUM_ID IN (SELECT ALBUM_ID FROM ALBUM WHERE USER_ID = :userId)", nativeQuery = true)
    void deleteAlbumPhotosByUserId(@Param("userId") Long userId);

    // 앨범 태그 삭제
    @Modifying
    @Query(value = "DELETE FROM ALBUM_TAGS WHERE ALBUM_ID IN (SELECT ALBUM_ID FROM ALBUM WHERE USER_ID = :userId)", nativeQuery = true)
    void deleteAlbumTagsByUserId(@Param("userId") Long userId);

    // 앨범 삭제
    @Modifying
    @Query(value = "DELETE FROM ALBUM WHERE USER_ID = :userId", nativeQuery = true)
    void deleteAlbumsByUserId(@Param("userId") Long userId);

    // 알림 삭제
    @Modifying
    @Query(value = "DELETE FROM NOTIFICATION WHERE USER_ID = :userId", nativeQuery = true)
    void deleteNotificationsByUserId(@Param("userId") Long userId);

    // QnA 댓글 삭제
    @Modifying
    @Query(value = "DELETE FROM QNA_COMMENTS WHERE USER_ID = :userId", nativeQuery = true)
    void deleteQnaCommentsByUserId(@Param("userId") Long userId);

    // QnA 게시글 삭제
    @Modifying
    @Query(value = "DELETE FROM QNA_POSTS WHERE USER_ID = :userId", nativeQuery = true)
    void deleteQnaPostsByUserId(@Param("userId") Long userId);

    // 사진 삭제
    @Modifying
    @Query(value = "DELETE FROM PHOTO WHERE USER_ID = :userId", nativeQuery = true)
    void deletePhotosByUserId(@Param("userId") Long userId);

    // 유저 삭제 (native query)
    @Modifying
    @Query(value = "DELETE FROM USERS WHERE USER_ID = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);

}
