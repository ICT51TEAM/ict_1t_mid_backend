package com.example.backend.user.dto;

import com.example.backend.user.entity.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [사용자 프로필 DTO (User Profile)]
 * - 프론트엔드로 사용자 정보를 전달할 때 사용하는 응답 DTO
 * - User 엔티티의 민감 정보(password 등)를 제외하고 필요한 정보만 포함
 *
 * [필요한 변수]
 * 1. id (Long) : 사용자 고유 ID
 * 2. email (String) : 이메일
 * 3. username (String) : 닉네임
 * 4. profileImageUrl (String) : 프로필 이미지 URL
 * 5. statusMessage (String) : 상태 메시지
 * 6. provider (String) : 가입 경로 ("LOCAL" / "KAKAO")
 * 7. visibility (String) : 공개 범위 ("PUBLIC" / "FRIENDS" / "PRIVATE")
 *
 * [사용 위치]
 * - AuthResponseDto 내부의 user 필드
 * - GET /api/users/me 응답
 * - GET /api/users/{userId} 응답
 *
 * [변환 예시] (서비스 레이어에서)
 * UserProfileDto dto = UserProfileDto.builder()
 * .id(user.getId())
 * .email(user.getEmail())
 * .username(user.getUsername())
 * .profileImageUrl(user.getProfileImageUrl())
 * .statusMessage(user.getStatusMessage())
 * .provider(user.getProvider())
 * .visibility(user.getVisibility())
 * .build();
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UserProfileDto {
    private Long id;
    private String email;
    private String username;
    private String profileImageUrl;
    private String statusMessage;
    private String provider;
    private String visibility;
    
    // JPQL 'select new' 쿼리를 위한 생성자 추가
    public UserProfileDto(Long id, String email, String username, String profileImageUrl, 
                          String statusMessage, String provider, String visibility) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.statusMessage = statusMessage;
        this.provider = provider;
        this.visibility = visibility;
    }
    
    // 내 프로필 조회 프론트에서 요구하는 필드들 추가
    private long joinDays;     // 가입 경과일
    private long albumCount;   // 앨범 수
    private long friendCount;  // 글벗 수
    private long totalBadges;  // 총 달개
    private long badgeTypes;   // 달개 종류
    
    
	public static UserProfileDto from(UserEntity user) {
		// 가입 경과일 계산 (현재 시간 - 가입 시간)
		long days = 0;
	    if (user.getCreatedAt() != null) {
	        days = java.time.temporal.ChronoUnit.DAYS
	        		.between(user.getCreatedAt(), java.time.LocalDateTime.now());
	    }
		
		return UserProfileDto.builder()
			   .id(user.getId())
			   .email(user.getEmail())
			   .username(user.getUsername())
			   .profileImageUrl(user.getProfileImageUrl())
			   .statusMessage(user.getStatusMessage())
			   .provider(user.getProvider())
			   .visibility(user.getVisibility())
			   	// ─── 통계 데이터 매핑 ───
	           .joinDays(days)
	           // 아래 데이터들은 연관관계 리스트의 size()를 이용하거나 0으로 초기화 후 추후 구현
	           //.albumCount(0)
	           //.friendCount(0)
	           //.totalBadges(0)
	           //.badgeTypes(0)
	           .build();
	}

}
