package com.example.backend.auth.dto;

import com.example.backend.user.dto.UserProfileDto.UserProfileDtoBuilder;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [로그인 요청 DTO (Login Request)]
 * - 프론트엔드 authService.login()에서 전송하는 데이터 구조
 * - POST /api/auth/login 의 @RequestBody로 바인딩됩니다.
 *
 * [필요한 변수]
 * 1. USER_ID(long) : 유니크한 시퀀스
 * 2. email (String) : 사용자 이메일 (로그인 ID로 사용)
 * 3. password (String) : 사용자 비밀번호 (평문 → 서버에서 BCrypt 비교)
 *
 * [사용 어노테이션]
 * - @Getter : Lombok — getter 자동 생성
 * - @NoArgsConstructor : Lombok — 기본 생성자 (Jackson JSON 역직렬화에 필요)
 */
@Getter
@NoArgsConstructor
public class LoginRequestDto {
	private long id;
    private String email;
    private String password;


}
