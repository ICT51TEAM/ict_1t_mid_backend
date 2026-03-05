package com.example.backend.user.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.backend.user.dto.ChangePasswordDto;
import com.example.backend.user.dto.UpdateProfileDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "사용자 API", description = "사용자 프로필 조회, 수정, 비밀번호 변경 및 계정 탈퇴 시 사용하는 API입니다.")
public interface UserControllerDocs {

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 반환합니다.")
    ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId);

    @Operation(summary = "특정 사용자 프로필 조회", description = "사용자 ID를 통해 특정 사용자의 프로필 정보를 조회합니다.")
    ResponseEntity<?> getUserProfile(@PathVariable Long userId);

    @Operation(summary = "내 프로필 업데이트", description = "현재 로그인한 사용자의 프로필 (이름, 상태메시지 등)을 수정합니다.")
    ResponseEntity<?> updateMyProfile(@RequestBody UpdateProfileDto request, @AuthenticationPrincipal Long userId);

    @Operation(summary = "비밀번호 변경", description = "사용자의 기존 비밀번호를 새 비밀번호로 변경합니다.") 
    ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto request, @AuthenticationPrincipal Long userId) throws IllegalAccessException;

    @Operation(summary = "회원 탈퇴", description = "본인 인증 및 비밀번호 확인을 거쳐 사용자를 시스템에서 삭제합니다.")
    ResponseEntity<?> deleteAccount(@RequestBody Map<String, String> body, @AuthenticationPrincipal Long userId);
}
