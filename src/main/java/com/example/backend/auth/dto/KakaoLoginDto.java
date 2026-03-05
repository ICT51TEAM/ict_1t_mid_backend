package com.example.backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [카카오 로그인 DTO (Kakao Login)]
 * - 프론트엔드 KakaoCallback.jsx에서 카카오 인가 코드를 백엔드로 전달할 때 사용
 * - POST /api/auth/kakao/login 의 @RequestBody
 *
 * [필요한 변수]
 * 1. kakaoCode (String) : 카카오 OAuth 인가 코드
 * - 프론트엔드가 카카오 인증 페이지에서 리다이렉트 후 URL 파라미터로 받은 code 값
 * - 이 코드를 백엔드에서 카카오 API에 전달하여 액세스 토큰 → 사용자 정보를 가져옴
 *
 * [카카오 로그인 전체 흐름]
 * 1. 프론트엔드: 카카오 OAuth URL로 리다이렉트
 * 2. 카카오: 사용자 인증 후 /auth/kakao/callback?code=XXXX 로 리다이렉트
 * 3. 프론트엔드(KakaoCallback): code를 백엔드 /api/auth/kakao/login 으로 POST
 * 4. 백엔드(KakaoService):
 * - 인가코드 → 카카오 토큰 API 호출 → 액세스 토큰 획득
 * - 액세스 토큰 → 카카오 사용자 정보 API 호출 → 이메일/닉네임 획득
 * - DB에 해당 이메일 유저 조회 → 없으면 자동 회원가입 (provider="KAKAO")
 * - 세션/토큰 발급 → AuthResponseDto 반환
 *
 * [사용 어노테이션]
 * - @Getter, @NoArgsConstructor
 */
@Getter
@NoArgsConstructor
public class KakaoLoginDto {
    private String kakaoCode;
    private Long kakaoId;
    private String username;
}
