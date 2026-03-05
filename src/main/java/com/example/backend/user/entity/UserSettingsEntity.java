package com.example.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * [사용자 설정 엔티티 (UserSettings Entity)]
 * - User와 1:1 관계로 미니홈피 설정 정보를 저장
 * - 오라클 DB의 USER_SETTINGS 테이블과 매핑
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Sequence
 * 2. user (User) : FK, User 엔티티와 1:1 매핑
 * - @OneToOne(fetch = FetchType.LAZY)
 * - @JoinColumn(name = "user_id", nullable = false, unique = true)
 *
 * 3. darkMode (boolean) : 다크 모드 활성화 (0/1)
 * 4. friendOnlyFeed (boolean) : 나만보기 설정 (0/1)
 * 5. pushNotification (Boolean) : 기본 푸시 알림
 * 6. commentNotification (Boolean) : 댓글 알림
 * 7. friendNotification (Boolean) : 친구 등록 알림
 * -> (기존 bgmUrl, showVisitorCount 관련 필드 등은 DB에 없어 @Transient 처리됨)
 *
 * [프론트엔드 연동 - userService.js]
 * - GET /api/users/me/settings → 이 엔티티 조회
 * - PUT /api/users/me/settings → 이 엔티티 수정
 *
 * [사용 어노테이션]
 * - @Entity, @Table(name = "USER_SETTINGS")
 * - @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder
 */
@Entity
@Table(name = "USER_SETTINGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_SETTINGS_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "DARK_MODE", nullable = false)
    private boolean darkMode;

    @Column(name = "FRIEND_ONLY_FEED", nullable = false)
    private boolean friendOnlyFeed;

    @Column(name = "PUSH_NOTIFICATION")
    private Boolean pushNotification;

    @Column(name = "COMMENT_NOTIFICATION")
    private Boolean commentNotification;

    @Column(name = "FRIEND_NOTIFICATION")
    private Boolean friendNotification;

    @Transient
    private String bgmUrl;

    @Transient
    private boolean showVisitorCount;

    @Transient
    private String themeColor;

    @Transient
    private boolean notificationEnabled;

    @Transient
    private boolean labFeaturesEnabled;
}
