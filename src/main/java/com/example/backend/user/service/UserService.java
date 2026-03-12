package com.example.backend.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.auth.repository.PasswordResetTokenRepository;
import com.example.backend.auth.repository.RefreshTokenRepository;
import com.example.backend.badge.repository.BadgeRepository;
import com.example.backend.friend.repository.FriendshipRepository;

import com.example.backend.user.dto.UpdateProfileDto;
import com.example.backend.user.dto.UserProfileDto;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

import com.example.backend.user.dto.UserSettingsDto;
import com.example.backend.user.entity.UserSettingsEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * [사용자 서비스 (User Service)]
 * - 프로필 조회/수정, 프로필 이미지 업로드, 비밀번호 변경, 회원탈퇴, 설정 관리
 *
 * [필요한 주입 객체]
 * 1. UserRepository userRepository : 사용자 CRUD
 * 2. UserSettingsRepository settingsRepository : (선택) 설정 별도 레포지토리
 * 3. PasswordEncoder passwordEncoder : 비밀번호 암호화/비교
 *
 * [구현해야 할 메서드]
 *
 * 1. getProfile(Long userId) → UserProfileDto
 * - userRepository.findById(userId).orElseThrow(() -> new
 * RuntimeException("사용자를 찾을 수 없습니다"))
 * - User → UserProfileDto 변환
 * - 변환 방법: new UserProfileDto(user.getId(), user.getEmail(), ...) 또는 빌더 패턴
 *
 * 2. updateProfile(Long userId, UpdateProfileDto dto) → void
 * - findById → username, visibility 업데이트 → save()
 *
 * 3. uploadProfileImage(Long userId, MultipartFile file) → String (이미지 URL)
 * - 파일 저장 경로: "./uploads/profiles/"
 * - 파일명 생성: UUID.randomUUID() + "." + 확장자
 * - Files.copy(file.getInputStream(), targetPath,
 * StandardCopyOption.REPLACE_EXISTING)
 * - user.setProfileImageUrl("/uploads/profiles/" + 파일명)
 * - save() 후 URL 반환
 *
 * 4. changePassword(Long userId, String currentPassword, String newPassword) →
 * void
 * - 현재 비밀번호 검증: passwordEncoder.matches(currentPassword, user.getPassword())
 * - 불일치 시: RuntimeException("현재 비밀번호가 일치하지 않습니다")
 * - user.setPassword(passwordEncoder.encode(newPassword))
 *
 * 5. deleteAccount(Long userId, String password) → void
 * - 비밀번호 검증 → 연관 데이터 처리 → userRepository.delete(user)
 *
 * 6. getSettings(Long userId) → UserSettingsDto
 * - UserSettings 조회 → DTO 변환
 *
 * 7. updateSettings(Long userId, UserSettingsDto dto) → void
 * - UserSettings 조회 (없으면 생성) → 필드 업데이트 → save()
 *
 * [사용 어노테이션]
 * - @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
 * - 데이터 변경 메서드에는 @Transactional 추가
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    // <리포지토리 주입>

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendshipRepository friendshipRepository;
    private final BadgeRepository badgeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PersistenceContext
    private EntityManager entityManager;

    private static final long MAX_PROFILE_IMAGE_SIZE = 10L * 1024L * 1024L;
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    public UserProfileDto getProfileById(Long userId) {
        // userId를 db에서 찾기
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다"));

        UserProfileDto dto = UserProfileDto.from(user);

        // 달개 통계 세팅 (받은 달개 집계)
        var badgeCounts = badgeRepository.countByUserIdGroupByTypeId(userId);
        long total = badgeCounts.stream().mapToLong(bc -> bc.getCount()).sum();
        dto.setTotalBadges(total);
        dto.setBadgeTypes(badgeCounts.size());

        return dto;
    }

    // * 1. getProfile(Long userId) → UserProfileDto
    // * - userRepository.findById(userId).orElseThrow(() -> new
    // RuntimeException("사용자를 찾을 수 없습니다"))
    // - User → UserProfileDto 변환
    // - 변환 방법: new UserProfileDto(user.getId(), user.getEmail(), ...) 또는 빌더 패턴
    @Transactional
    public UserProfileDto getProfile(long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        UserProfileDto dto = UserProfileDto.from(user);
        var badgeCounts = badgeRepository.countByUserIdGroupByTypeId(userId);
        long total = badgeCounts.stream().mapToLong(bc -> bc.getCount()).sum();
        dto.setTotalBadges(total);
        dto.setBadgeTypes(badgeCounts.size());
        return dto;
    }

    public UserProfileDto getProfileByEmail(String name) {
        UserEntity user = userRepository.findByEmail(name)
                .orElseThrow(() -> new IllegalArgumentException("프로필 정보를 찾을 수가 없습니다"));
        return UserProfileDto.from(user);
    }

    // 2. updateProfile(Long userId, UpdateProfileDto dto) → void
    // - findById → username, visibility 업데이트 → save()
    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileDto request) {
        // 유저 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        // 데이터 변경
        if (request.getUsername() != null)
            user.setUsername(request.getUsername());
        if (request.getVisibility() != null)
            user.setVisibility(request.getVisibility());

        // 결과 DTO 반영후 반환
        return UserProfileDto.from(user);
    }

    // @Transactional
    // public void updateProfile(Long userId, UpdateProfileDto dto) {
    // // 여기에 프로필 수정 로직을 작성하세요.
    // }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }
        String ext = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 확장자입니다. (jpg, jpeg, png, webp만 가능)");
        }

        Path profileDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("profiles");
        try {
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지 폴더를 생성할 수 없습니다.", e);
        }

        String cleanedName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "profile" : file.getOriginalFilename()).replace(" ", "_");
        String savedFileName = UUID.randomUUID() + "_" + cleanedName;
        Path target = profileDir.resolve(savedFileName);

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        String imageUrl = "/uploads/profiles/" + savedFileName;
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);

        return imageUrl;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null)
            return "";
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1)
            return "";
        return fileName.substring(idx + 1).toLowerCase();
    }

    // @Transactional
    // public void changePassword(Long userId, String currentPassword, String
    // newPassword) {
    // // 여기에 비밀번호 변경 로직을 작성하세요.
    // }
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수가 없습니다"));

        // 카카오 유저 막기
        if ("KAKAO".equalsIgnoreCase(user.getProvider())) {
            throw new IllegalArgumentException("카카오 로그인 계정은 비밀번호 변경이 불가능합니다.");
        }

        // 현재 비번 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비번 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // @Transactional
    // public void deleteAccount(Long userId, String password) {
    // // 여기에 회원 탈퇴 로직을 작성하세요.
    @Transactional
    public void deleteAccount(Long userid, String password) {
        UserEntity user = userRepository.findById(userid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        // 카카오 로그인 유저는 비밀번호가 없거나 설정된 비밀번호가 다를 수 있기 때문에 다르게 처리
        if (!"KAKAO".equalsIgnoreCase(user.getProvider())) {
            // 입력한 비밀번호와 DB에 암호화되어 저장된 비밀번호가 일치하는지 검증
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
            }
        }

        // ── [현재 방식] JPA 레포지토리 + try-catch ─────────────────────────────────
        // ※ 주의: @Transactional 안에서 JPA 예외 발생 시 Hibernate 세션이 깨질 수 있음
        // → catch 해도 세션 복구 불가할 수 있으니, 삭제 순서를 정확히 지켜야 함
        //
        // ── [실제 DB 테이블 목록] ────────────────────────────────────────────────────
        // ALBUM, ALBUM_DALGAE, ALBUM_PHOTO, ALBUM_TAGS, BADGE_TYPES, BADGES,
        // DALGAE_TYPES, EMAIL_VERIFICATION_TOKEN, FRIENDSHIPS, LAYOUT_TYPE_CODE,
        // NOTIFICATION, PASSWORD_RESET_TOKEN, PHOTO, QNA_COMMENTS, QNA_POSTS,
        // TAGS, USER_SETTINGS, USERS, VISIBILITY_CODE
        Long uid = user.getId();

        // 1. 친구 관계 삭제
        try {
            friendshipRepository.deleteAllByUserId(uid);
        } catch (Exception e) {
            System.out.println("친구 삭제 스킵: " + e.getMessage());
        }

        // 2. 앨범 달개 삭제 (유저 앨범에 달린 달개 + 유저가 남긴 달개)
        try {
            userRepository.deleteAlbumDalgaeByUserId(uid);
        } catch (Exception e) {
            System.out.println("앨범 달개 삭제 스킵: " + e.getMessage());
        }

        // 3. 앨범 사진 삭제
        try {
            userRepository.deleteAlbumPhotosByUserId(uid);
        } catch (Exception e) {
            System.out.println("앨범 사진 삭제 스킵: " + e.getMessage());
        }

        // 4. 앨범 태그 삭제
        try {
            userRepository.deleteAlbumTagsByUserId(uid);
        } catch (Exception e) {
            System.out.println("앨범 태그 삭제 스킵: " + e.getMessage());
        }

        // 5. 뱃지 삭제 (유저가 남긴 뱃지)
        try {
            badgeRepository.deleteByUserId(uid); // 내 앨범에 달린 달개
            badgeRepository.deleteByGivenUserId(uid); // 내가 남의 앨범에 남긴 달개
        } catch (Exception e) {
            System.out.println("뱃지 삭제 스킵: " + e.getMessage());
        }

        // 6. 앨범 삭제 (FK_ALBUM_USER 제약조건 해소)
        try {
            userRepository.deleteAlbumsByUserId(uid);
        } catch (Exception e) {
            System.out.println("앨범 삭제 스킵: " + e.getMessage());
        }

        // 7. 알림 삭제
        try {
            userRepository.deleteNotificationsByUserId(uid);
        } catch (Exception e) {
            System.out.println("알림 삭제 스킵: " + e.getMessage());
        }

        // 8. QnA 댓글 삭제 (QNA_POSTS보다 먼저 삭제)
        try {
            userRepository.deleteQnaCommentsByUserId(uid);
        } catch (Exception e) {
            System.out.println("QnA 댓글 삭제 스킵: " + e.getMessage());
        }

        // 9. QnA 게시글 삭제
        try {
            userRepository.deleteQnaPostsByUserId(uid);
        } catch (Exception e) {
            System.out.println("QnA 게시글 삭제 스킵: " + e.getMessage());
        }

        // 10. 사진 삭제
        try {
            userRepository.deletePhotosByUserId(uid);
        } catch (Exception e) {
            System.out.println("사진 삭제 스킵: " + e.getMessage());
        }

        // 11. 비밀번호 재설정 토큰 삭제
        try {
            passwordResetTokenRepository.deleteByUserId(uid);
        } catch (Exception e) {
            System.out.println("토큰 삭제 스킵: " + e.getMessage());
        }

        // 11-1. 리프레시 토큰 삭제
        try {
            refreshTokenRepository.deleteByUserId(uid);
        } catch (Exception e) {
            System.out.println("리프레시 토큰 삭제 스킵: " + e.getMessage());
        }

        // 12. 유저 설정 삭제
        try {
            userRepository.deleteSettingsByUserId(uid);
        } catch (Exception e) {
            System.out.println("설정 삭제 스킵: " + e.getMessage());
        }

        // 13. 유저 삭제
        userRepository.deleteByUserId(uid);

        // ── [이전 방식] EntityManager 네이티브 SQL ────────────────────────────────
        // JPA 세션과 무관하게 직접 SQL 실행 → 개별 실패해도 세션이 깨지지 않음
        // 사용하려면 상단의 EntityManager, @PersistenceContext 임포트/필드 주석 해제 필요
        //
        // Long uid = user.getId();
        // entityManager.createNativeQuery("DELETE FROM FRIENDSHIPS WHERE REQUESTER_ID =
        // ?1 OR ACCEPTER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery(
        // "DELETE FROM ALBUM_DALGAE WHERE GIVEN_BY_USER_ID = ?1 OR ALBUM_ID IN (SELECT
        // ALBUM_ID FROM ALBUM WHERE USER_ID = ?1)")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery(
        // "DELETE FROM ALBUM_PHOTO WHERE ALBUM_ID IN (SELECT ALBUM_ID FROM ALBUM WHERE
        // USER_ID = ?1)")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery(
        // "DELETE FROM ALBUM_TAGS WHERE ALBUM_ID IN (SELECT ALBUM_ID FROM ALBUM WHERE
        // USER_ID = ?1)")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM BADGES WHERE USER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM ALBUM WHERE USER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM NOTIFICATION WHERE USER_ID =
        // ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM QNA_COMMENTS WHERE USER_ID =
        // ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM QNA_POSTS WHERE USER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM PHOTO WHERE USER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM PASSWORD_RESET_TOKEN WHERE
        // USER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM USER_SETTINGS WHERE USER_ID =
        // ?1")
        // .setParameter(1, uid).executeUpdate();
        // entityManager.createNativeQuery("DELETE FROM USERS WHERE USER_ID = ?1")
        // .setParameter(1, uid).executeUpdate();
    }

    // public UserSettingsDto getSettings(Long userId) {
    // // 여기에 설정 조회 로직을 작성하세요.
    // }

    // @Transactional
    // public void updateSettings(Long userId, UserSettingsDto dto) {
    // // 여기에 설정 수정 로직을 작성하세요.
    // }

    // public UserSettingsDto getSettings(Long userId) {
    // // 여기에 설정 조회 로직을 작성하세요.
    // }

    // @Transactional
    // public void updateSettings(Long userId, UserSettingsDto dto) {
    // // 여기에 설정 수정 로직을 작성하세요.
    // }
    @Transactional
    public UserSettingsDto getSettings(Long userId) {
        UserSettingsEntity settings = entityManager
                .createQuery("SELECT s FROM UserSettingsEntity s WHERE s.user.id = :userId", UserSettingsEntity.class)
                .setParameter("userId", userId)
                .getResultStream().findFirst()
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
                    UserSettingsEntity newSettings = UserSettingsEntity.builder()
                            .user(user)
                            .pushNotification(true)
                            .build();
                    entityManager.persist(newSettings);
                    return newSettings;
                });
        return UserSettingsDto.builder()
                .notificationEnabled(Boolean.TRUE.equals(settings.getPushNotification()))
                .build();
    }

    @Transactional
    public UserSettingsDto updateSettings(Long userId, UserSettingsDto dto) {
        UserSettingsEntity settings = entityManager
                .createQuery("SELECT s FROM UserSettingsEntity s WHERE s.user.id = :userId", UserSettingsEntity.class)
                .setParameter("userId", userId)
                .getResultStream().findFirst()
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
                    UserSettingsEntity newSettings = UserSettingsEntity.builder()
                            .user(user).build();
                    entityManager.persist(newSettings);
                    return newSettings;
                });
        settings.setPushNotification(dto.isNotificationEnabled());
        return dto;
    }

}
