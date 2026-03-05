package com.example.backend.auth.entity;

import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [이메일 인증 토큰 엔티티 (EmailVerificationToken)]
 * - 회원가입 시 이메일 인증을 위해 발송한 6자리 코드를 저장하는 테이블
 * - 오라클 DB의 EMAIL_VERIFICATION_TOKENS 테이블과 매핑
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Sequence (@GeneratedValue(strategy =
 * GenerationType.SEQUENCE))
 * 2. email (String) : 인증 코드를 발송한 이메일 주소 -> @Column(name = "EMAIL")
 * 3. code (String) : 6자리 랜덤 인증 코드 (예: "482917") -> @Column(name = "CODE")
 * 4. expiresAt (LocalDateTime) : 인증 코드 만료 시간 (보통 발송 시각 + 5분) -> @Column(name =
 * "EXPIRES_AT")
 * 5. used (boolean) : 이미 사용(인증 완료)된 코드인지 여부 -> @Column(name = "USED_AT") 으로 대체됨
 * 6. createdAt (LocalDateTime) : 레코드 생성 시각 -> @Column(name = "CREATED_AT")
 *
 * [비즈니스 로직 힌트]
 * - 인증 코드 검증 시: email + code 일치 && expiresAt > 현재시각 && used == false 인지 확인
 * - 인증 성공 시: used = true 로 업데이트 (이제는 내부적으로 setUsed(true) 를 통해 USED_AT 기록)
 *
 * [사용 어노테이션]
 * - @Entity, @Table(name = "EMAIL_VERIFICATION_TOKEN")
 * - @Id, @GeneratedValue(strategy = GenerationType.IDENTITY)
 */
@Entity
@Table(name = "EMAIL_VERIFICATION_TOKEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMAIL_VERIFICATION_TOKEN_ID")
    private Long id;

    @Column(name = "EMAIL", nullable = false, length = 255)
    private String email;

    @Column(name = "CODE", nullable = false, length = 10)
    private String code;

    @Column(name = "EXPIRES_AT", nullable = false)
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
