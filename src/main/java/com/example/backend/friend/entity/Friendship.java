package com.example.backend.friend.entity;

import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [친구 관계 맵핑 엔티티 (Friendship Entity)]
 * - USERS 와 USERS 양쪽을 N:M 이어주어 친구 관계(+요청 상태)를 관리하는 중간 테이블
 * - 오라클 DB의 FRIENDSHIPS 테이블과 매핑
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Sequence
 *
 * 2. fromUser (User) : 친구 요청을 보낸(신청한) 유저
 * - @ManyToOne(fetch = FetchType.LAZY)
 * - @JoinColumn(name = "REQUESTER_ID", nullable = false)
 *
 * 3. toUser (User) : 친구 요청을 받은 대상 유저
 * - @ManyToOne(fetch = FetchType.LAZY)
 * - @JoinColumn(name = "ACCEPTER_ID", nullable = false)
 *
 * 4. status (String 또는 Enum) : 현재 관계 상태
 * - "PENDING" : 요청 대기중
 * - "ACCEPTED" : 친구 수락됨
 * - "REJECTED" : 거절됨
 * - "BLOCKED" : 차단됨 (선택사항)
 *
 * 5. createdAt (LocalDateTime) : 친구 요청 보낸 시간
 * 6. updatedAt (LocalDateTime) : 수락/거절된 시간
 *
 * [비즈니스 로직 힌트]
 * - "내 친구 목록" 조회 시:
 * (fromUser = 나 AND status = ACCEPTED) OR (toUser = 나 AND status = ACCEPTED)
 * → 양방향 모두 검색해야 함
 *
 * - "받은 친구 요청" 조회 시:
 * toUser = 나 AND status = PENDING
 *
 * - 친구 요청 수락: status를 PENDING → ACCEPTED 로 변경
 * - 친구 요청 거절: status를 PENDING → REJECTED 로 변경
 * - 친구 삭제: 해당 Friendship 레코드를 DB에서 삭제
 *
 * [사용 어노테이션]
 * - @Entity, @Table(name = "FRIENDSHIPS")
 * - @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder
 */
@Entity
@Table(name = "FRIENDSHIPS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FRIENDSHIP_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REQUESTER_ID", nullable = false)
    private UserEntity fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCEPTER_ID", nullable = false)
    private UserEntity toUser;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status; // "PENDING", "ACCEPTED", "REJECTED", "BLOCKED"

    @Column(name = "REQUESTED_AT")
    private LocalDateTime createdAt;

    @Column(name = "RESPONDED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
