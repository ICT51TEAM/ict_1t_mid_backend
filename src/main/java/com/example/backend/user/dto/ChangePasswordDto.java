package com.example.backend.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [비밀번호 변경 DTO (Change Password)]
 * - 프론트엔드 userService.changePassword(currentPassword, newPassword) 에서 전송
 * - PUT /api/users/me/password 의 @RequestBody
 *
 * [필요한 변수]
 * 1. currentPassword (String) : 현재 비밀번호 (서버에서 DB값과 BCrypt 비교 검증)
 * 2. newPassword (String) : 새 비밀번호 (서버에서 BCrypt 암호화 후 저장)
 *
 * [보안 주의사항]
 * - currentPassword가 DB의 암호화된 비밀번호와 일치하는지 반드시 검증
 * - 카카오 로그인 유저(provider="KAKAO")는 비밀번호 변경 불가 처리 권장
 */
@Getter
@NoArgsConstructor
public class ChangePasswordDto {
    private String email;
    private String currentPassword;
    private String newPassword;
}
