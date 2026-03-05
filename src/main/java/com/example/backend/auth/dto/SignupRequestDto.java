package com.example.backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [회원가입 요청 DTO (Signup Request)]
 * - 프론트엔드 authService.signup()에서 전송하는 데이터 구조
 * - POST /api/auth/signup 의 @RequestBody로 바인딩됩니다.
 *
 * [필요한 변수]
 * 1. email (String) : 가입할 이메일 (중복 검사 필요)
 * 2. password (String) : 비밀번호 (서버에서 BCryptPasswordEncoder.encode()로 암호화 후 저장)
 * 3. username (String) : 닉네임 (프로필에 표시될 이름)
 *
 * [사용 어노테이션]
 * - @Getter, @NoArgsConstructor
 */
@Getter
@NoArgsConstructor
public class SignupRequestDto {
    private String email;
    private String password;
    private String username;
    private String provider;
    private String visibility;
}
