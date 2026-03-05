package com.example.backend.auth.repository;

import com.example.backend.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * [이메일 인증 토큰 레포지토리 (EmailVerificationToken Repository)]
 * - EmailVerificationToken 엔티티에 대한 DB 조회/저장을 담당
 *
 * [구현해야 할 커스텀 쿼리 메서드]
 * 1. findByEmailAndCodeAndUsedFalse(String email, String code)
 * → 이메일 + 코드 일치 + 아직 미사용(used=false)인 토큰 조회
 * → 인증 코드 검증 시 사용
 *
 * 2. findTopByEmailOrderByCreatedAtDesc(String email)
 * → 해당 이메일로 가장 최근 발송된 인증 토큰 조회
 * → 중복 발송 방지 등에 활용 가능
 *
 * [사용 방법]
 * - JpaRepository<EmailVerificationToken, Long>을 상속하면
 * save(), findById(), delete() 등 기본 CRUD는 자동 제공됩니다.
 * - 위의 메서드명을 선언하기만 하면 Spring Data JPA가 이름 기반으로 자동 쿼리 생성
 */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByEmailAndCodeAndUsedAtIsNull(String email, String code);

    Optional<EmailVerificationToken> findTopByEmailOrderByCreatedAtDesc(String email);
}
