package com.example.backend.auth.dto;

import com.example.backend.user.dto.UserProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [인증 응답 DTO (Auth Response)]
 * - 로그인/회원가입 성공 시 프론트엔드로 반환하는 데이터 구조
 * - 프론트엔드 AuthContext에서 login(token, userData) 형태로 사용
 *
 * [필요한 변수]
 * 1. token (String) : 인증 토큰 (프론트엔드가 localStorage에 저장 후 모든 API 요청에 Authorization
 * 헤더로 전송)
 * 2. user (UserProfileDto) : 로그인한 사용자 기본 정보 (id, email, username,
 * profileImageUrl 등)
 *
 * [사용 어노테이션]
 * - @Getter : getter 자동 생성
 * - @AllArgsConstructor : 모든 필드를 인자로 받는 생성자
 * - @Builder : 빌더 패턴 (서비스 레이어에서
 * AuthResponseDto.builder().token(...).user(...).build() 형태로 생성)
 */
//@Data // Getter, Setter, RequiredArgsConstructor 등을 모두 포함
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String refreshToken; // 이 이름이 정확히 일치해야 합니다.
    private UserProfileDto user;

}