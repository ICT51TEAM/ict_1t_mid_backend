package com.example.backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [이메일 인증 코드 검증 DTO (Email Verify)]
 * - 프론트엔드 authService.verifyEmailCode(email, code)에서 전송하는 데이터 구조
 * - POST /api/auth/email/verify-code 의 @RequestBody
 *
 * [필요한 변수]
 * 1. email (String) : 인증번호를 받은 이메일 주소
 * 2. code (String) : 사용자가 입력한 6자리 인증 코드
 *
 * [사용 어노테이션]
 * - @Getter, @NoArgsConstructor
 */
@Getter
@NoArgsConstructor
public class EmailVerifyDto {
    private String email;
    private String code;
}
