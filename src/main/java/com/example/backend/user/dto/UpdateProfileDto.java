package com.example.backend.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [프로필 수정 DTO (Update Profile)]
 * - 프론트엔드 userService.updateProfile(username, visibility) 에서 전송
 * - PUT /api/users/me 의 @RequestBody
 *
 * [필요한 변수]
 * 1. username (String) : 변경할 닉네임
 * 2. visibility (String) : 변경할 공개 범위 ("PUBLIC" / "FRIENDS" / "PRIVATE")
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileDto {
    private String username;
    private String visibility;
}
