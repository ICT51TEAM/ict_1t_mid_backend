package com.example.backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [이메일 요청 DTO (Email Request)]
 * - 프론트엔드 authService.sendEmailCode(email)에서 전송하는 데이터 구조
 * - POST /api/auth/email/send-code 및 POST /api/auth/email/send-reset-code
 * 의 @RequestBody
 *
 * [필요한 변수]
 * 1. email (String) : 인증번호를 받을 이메일 주소
 *
 * [사용 어노테이션]
 * - @Getter, @NoArgsConstructor
 */
@Getter
@NoArgsConstructor
public class EmailRequestDto {
    private String email;
}
