package com.example.backend.user.dto;

import lombok.*;

/**
 * [사용자 설정 DTO (User Settings)]
 * - 프론트엔드 userService.getSettings() / updateSettings() 에서 사용
 * - GET /api/users/me/settings 응답 및 PUT /api/users/me/settings 요청 본문
 *
 * [필요한 변수]
 * 1. bgmUrl (String) : 배경음악 URL
 * 2. showVisitorCount (boolean) : 방문자 수 표시 여부
 * 3. themeColor (String) : 미니홈피 테마 색상
 * 4. notificationEnabled (boolean) : 알림 ON/OFF
 * 5. labFeaturesEnabled (boolean) : 실험실 기능 ON/OFF
 *
 * [변환 예시] (서비스 레이어에서)
 * UserSettingsDto dto = UserSettingsDto.builder()
 * .bgmUrl(settings.getBgmUrl())
 * .showVisitorCount(settings.isShowVisitorCount())
 * .themeColor(settings.getThemeColor())
 * .notificationEnabled(settings.isNotificationEnabled())
 * .labFeaturesEnabled(settings.isLabFeaturesEnabled())
 * .build();
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsDto {
    private String bgmUrl;
    private boolean showVisitorCount;
    private String themeColor;
    private boolean notificationEnabled;
    private boolean labFeaturesEnabled;
}
