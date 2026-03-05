package com.example.backend.auth.repository;

import com.example.backend.auth.entity.PasswordResetToken;
import com.example.backend.user.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * [비밀번호 재설정 토큰 레포지토리 (PasswordResetToken Repository)]
 * - PasswordResetToken 엔티티에 대한 DB 조회/저장을 담당
 *
 * [구현해야 할 커스텀 쿼리 메서드]
 * 1. findByTokenHashAndUsedAtIsNull(String tokenHash)
 * → 토큰 해시(또는 코드)가 일치하고 미사용(used=false)인 토큰 조회
 * → 비밀번호 재설정 검증 시 사용
 *
 * 2. findTopByUserOrderByCreatedAtDesc(User user)
 * → 해당 유저에게 가장 최근 발급된 재설정 토큰 조회
 *
 * [사용 방법]
 * - JpaRepository를 상속 → 기본 CRUD 자동 제공
 * - 메서드명 규칙에 따라 선언만 하면 Spring Data JPA가 쿼리 자동 생성
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    Optional<PasswordResetToken> findTopByUserOrderByCreatedAtDesc(UserEntity user);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
