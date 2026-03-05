package com.example.backend.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.example.backend.global.entity.BaseEntity;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [사용자 엔티티 (User Entity)]
 * - 오라클 DB의 USERS 테이블과 매핑됩니다.
 * - BaseEntity를 상속받아 createdAt, updatedAt을 자동 관리합니다.
 *
 * [필요한 변수 (기본 키 및 컬럼)]
 * 1. id (Long) : PK, Oracle Sequence 전략
 * - @Id
 * - @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
 * - @SequenceGenerator(name = "user_seq", sequenceName = "USER_SEQ",
 * allocationSize = 1)
 *
 * 2. email (String) : 고유 계정 이메일 (로그인 ID 역할)
 * - @Column(nullable = false, unique = true)
 * - 일반 이메일 혹은 카카오 로그인 시 카카오에서 제공받는 이메일
 *
 * 3. password (String) : BCrypt 암호화된 비밀번호
 * - @Column(name = "PASSWORD_HASH") → 카카오 로그인 유저는 비밀번호가 없을 수 있음
 * - 일반가입: BCryptPasswordEncoder.encode(rawPassword) 결과 저장
 * - 카카오: null 또는 UUID 기반 임시 해시값
 *
 * 4. username (String) : 닉네임 (프로필에 표시)
 * - @Column(name = "USERNAME", nullable = false)
 *
 * 5. profileImageUrl (String) : 프로필 사진 URL 또는 서버 내 파일 경로
 * - @Column(name = "PROFILE_IMAGE_URL", length = 500)
 * - 기본값: null (프론트에서 기본 이미지 표시)
 *
 * 6. statusMessage (String) : 상태 메시지 (미니홈피 한줄 소개) -> DB에 없음
 * - @Transient 로 처리
 *
 * 7. provider (String) : 가입 경로 ("LOCAL" 또는 "KAKAO")
 * - @Column(name = "OAUTH_PROVIDER", nullable = false)
 * - 일반 회원가입: "LOCAL"
 * - 카카오 로그인: "KAKAO"
 * - 소셜/일반 구분에 활용 (비밀번호 변경 가능 여부 등)
 *
 * 8. oauthProviderId (String) : 카카오 로그인 ID
 * - @Column(name = "OAUTH_PROVIDER_ID", unique = true)
 *
 * 9. role (String) : 권한 (예: "ROLE_USER", "ROLE_ADMIN") -> DB에 없음
 * - @Transient 로 처리
 *
 * 10. visibility (String) : 미니홈피 공개 범위 ("PUBLIC", "FRIENDS", "PRIVATE")
 * - @Column(name = "VISIBILITY", nullable = false)
 * - 기본값: "PUBLIC"
 * 
 * 11. primaryDalgaeId (Long) : 대표 달개
 * - @Column(name = "PRIMARY_DALGAE")
 *
 * [상속]
 * - extends BaseEntity → createdAt, updatedAt 자동 관리
 *
 * [연관관계] (필요 시 추가)
 * - @OneToOne UserSettings : 미니홈피 설정
 * - @OneToMany List<Post> : 작성한 게시글 목록
 *
 * [사용 어노테이션]
 * - @Entity, @Table(name = "USERS")
 * - @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder
 */
@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    @Column(name = "EMAIL", unique = true)
    private String email;

    @Column(name = "PASSWORD_HASH")
    private String password;

    @Column(name = "USERNAME", nullable = false, length = 50)
    private String username;

    @Column(name = "PROFILE_IMAGE_URL", length = 500)
    private String profileImageUrl;

    @Transient
    private String statusMessage;

    @Column(name = "OAUTH_PROVIDER", nullable = false, length = 30)
    private String provider; // "LOCAL" or "KAKAO"

    @Column(name = "OAUTH_PROVIDER_ID", unique = true, length = 100)
    private String oauthProviderId;
    
    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Transient
    private String role = "ROLE_USER"; // "ROLE_USER" or "ROLE_ADMIN"

    @Column(name = "VISIBILITY", nullable = false, length = 20)
    private String visibility = "PUBLIC"; // "PUBLIC", "FRIENDS", "PRIVATE"

    @Column(name = "PRIMARY_DALGAE")
    private Long primaryDalgaeId;
    
    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    
    public Long getId() {
        return id;
    }

	public String getPassword() {
		return this.password; // 필드에 저장된 값 반환
	}

    // [연관관계] 필요 시 주석 해제
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch =
    // FetchType.LAZY)
    // private UserSettings userSettings;
	
	// [추가] 앨범과의 1:N 관계 설정
	//@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	//private List<Post> posts = new ArrayList<>();
}
