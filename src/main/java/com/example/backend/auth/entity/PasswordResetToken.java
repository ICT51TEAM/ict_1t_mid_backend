package com.example.backend.auth.entity;

import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [비밀번호 재설정 토큰 엔티티 (PasswordResetToken)]
 * - 비밀번호 재설정 요청 시 발송한 인증 코드를 저장하는 테이블
 * - 오라클 DB의 PASSWORD_RESET_TOKENS 테이블과 매핑
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Sequence -> @GeneratedValue(strategy =
 * GenerationType.IDENTITY)
 * 2. user (User) : FK, 비밀번호 재설정 대상 사용자 (@ManyToOne, @JoinColumn)
 * 3. tokenHash (String) : 토큰 해시값 -> @Column(name = "TOKEN_HASH")
 * 4. expiresAt (LocalDateTime) : 토큰 만료 시간 -> @Column(name = "EXPIRES_AT")
 * 5. used (boolean) : 이미 사용된 토큰인지 여부 -> @Column(name = "USED_AT") 으로 관리
 * 6. createdAt (LocalDateTime) : 레코드 생성 시각 -> @Column(name = "CREATED_AT")
 *
 * [비즈니스 로직 힌트]
 * - 비밀번호 재설정 요청 시: 해당 이메일의 User 조회 → 인증 코드 생성 → 이 엔티티에 저장 → 이메일 발송
 * - 재설정 실행 시: tokenHash/code 일치 && expiresAt > 현재시각 && used == false 확인 후 비밀번호
 * 업데이트
 *
 * [사용 어노테이션]
 * - @Entity, @Table(name = "PASSWORD_RESET_TOKEN")
 * - @ManyToOne(fetch = FetchType.LAZY) — User와 다대일 관계
 */
@Entity
@Table(name = "PASSWORD_RESET_TOKEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PASSWORD_RESET_TOKEN_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @Column(name = "TOKEN_HASH", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public void setUsed(boolean used) {
        if (used) {
            this.usedAt = LocalDateTime.now();
        } else {
            this.usedAt = null;
        }
    }
}
