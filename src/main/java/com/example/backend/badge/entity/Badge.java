package com.example.backend.badge.entity;

import com.example.backend.album.entity.AlbumEntity;
import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [달개(뱃지/좋아요) 엔티티 (Badge Entity)]
 * - 게시글에 달개(좋아요 반응)를 남기는 기능의 데이터 저장
 * - 오라클 DB의 BADGES 테이블과 매핑
 * - 누가(user) + 어떤 게시글에(post) + 어떤 종류의 달개를(badgeType) 남겼는지 기록
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Sequence
 *
 * 2. user (User) : FK, 달개를 남긴 사용자
 * - @ManyToOne(fetch = FetchType.LAZY)
 * - @JoinColumn(name = "user_id", nullable = false)
 *
 * 3. post (Post) : FK, 달개가 달린 게시글
 * - @ManyToOne(fetch = FetchType.LAZY)
 * - @JoinColumn(name = "post_id", nullable = false)
 *
 * 4. badgeType (BadgeType) : FK, 달개 유형 (좋아요, 슬퍼요, 화나요 등)
 * - @ManyToOne(fetch = FetchType.LAZY)
 * - @JoinColumn(name = "badge_type_id", nullable = false)
 *
 * 5. createdAt (LocalDateTime) : 달개를 남긴 시각
 *
 * [비즈니스 로직 힌트]
 * - 한 사용자가 같은 게시글에 동일 타입의 달개를 중복으로 남길 수 없음
 * → UNIQUE 제약조건: (user_id, post_id, badge_type_id)
 * - 달개 토글: 이미 있으면 삭제(취소), 없으면 생성(추가)
 *
 * [사용 어노테이션]
 * - @Entity, @Table(name = "BADGES", uniqueConstraints = ...)
 */
@Entity
@Table(name = "BADGES", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "ALBUM_ID", "badge_type_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "badge_seq")
    @SequenceGenerator(name = "badge_seq", sequenceName = "BADGE_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALBUM_ID", nullable = false)
    private AlbumEntity album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_type_id", nullable = false)
    private BadgeType badgeType;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
