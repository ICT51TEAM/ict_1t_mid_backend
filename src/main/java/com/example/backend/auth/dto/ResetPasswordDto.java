package com.example.backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [비밀번호 재설정 DTO (Reset Password)]
 * - 프론트엔드 authService.resetPassword(email, newPassword)에서 전송하는 데이터 구조
 * - POST /api/auth/reset-password 의 @RequestBody
 *
 * [필요한 변수]
 * 1. email (String) : 비밀번호를 재설정할 계정의 이메일
 * 2. newPassword (String) : 새 비밀번호 (서버에서 BCrypt 암호화 후 DB 업데이트)
 *
 * [사용 어노테이션]
 * - @Getter, @NoArgsConstructor
 */
@Getter
@NoArgsConstructor
public class ResetPasswordDto {
    private String email;
    private String newPassword;
}
