package com.example.backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

import com.example.backend.auth.dto.EmailRequestDto;
import com.example.backend.auth.dto.EmailVerifyDto;
import com.example.backend.auth.dto.KakaoLoginDto;
import com.example.backend.auth.dto.LoginRequestDto;
import com.example.backend.auth.dto.ResetPasswordDto;
import com.example.backend.auth.dto.SignupRequestDto;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증 API", description = "로그인, 회원가입, 로그아웃, 카카오 로그인 및 이메일 인증 관련 API입니다.")
public interface AuthControllerDocs {

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 토큰을 발급받습니다.", tags = { "1. 로그인 (최우선)" })
    ResponseEntity<?> login(@RequestBody LoginRequestDto credentials, HttpSession session);

    @Operation(summary = "회원가입", description = "새로운 계정을 생성합니다.")
    ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequestDto requestDto);

    @Operation(summary = "로그아웃", description = "현재 계정에서 로그아웃 처리합니다.")
    ResponseEntity<?> logout();

    @Operation(summary = "카카오 로그인", description = "카카오 계정으로 로그인합니다.", tags = { "1. 로그인 (최우선)" })
    ResponseEntity<?> kakaoLogin(@RequestBody KakaoLoginDto dto, HttpSession session);

    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입을 위한 이메일 인증 코드를 발송합니다.")
    ResponseEntity<?> sendEmailCode(@RequestBody EmailRequestDto requestDto);

    @Operation(summary = "이메일 인증 코드 검증", description = "이메일로 발송된 인증 코드를 확인합니다.")
    ResponseEntity<?> verifyEmailCode(@RequestBody EmailVerifyDto requestDto);

    @Operation(summary = "비밀번호 재설정 이메일 발송", description = "비밀번호를 재설정하기 위한 코드를 이메일로 발송합니다.")
    ResponseEntity<?> sendResetEmailCode(@RequestBody EmailRequestDto requestDto);

    @Operation(summary = "비밀번호 재설정 코드 검증", description = "비밀번호 재설정용 인증 코드를 확인합니다.")
    ResponseEntity<?> verifyResetCode(@RequestBody EmailVerifyDto requestDto);

    @Operation(summary = "비밀번호 재설정", description = "새로운 비밀번호로 변경합니다.")
    ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request);
}
