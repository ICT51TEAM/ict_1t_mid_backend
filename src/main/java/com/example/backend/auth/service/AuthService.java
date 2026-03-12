package com.example.backend.auth.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.auth.dto.AuthResponseDto;
import com.example.backend.auth.dto.SignupRequestDto;
import com.example.backend.auth.entity.EmailVerificationToken;
import com.example.backend.auth.entity.PasswordResetToken;
import com.example.backend.auth.entity.RefreshToken;
import com.example.backend.auth.repository.EmailVerificationTokenRepository;
import com.example.backend.auth.repository.PasswordResetTokenRepository;
import com.example.backend.auth.repository.RefreshTokenRepository;
import com.example.backend.global.config.JwtUtil;
import com.example.backend.user.dto.UserProfileDto;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [인증 서비스 (Auth Service)]
 * - 로그인, 회원가입, 로그아웃 등 인증 관련 실제 비즈니스 로직이 작성됩니다.
 * - 이메일 발송은 EmailService, 카카오 관련은 KakaoService에 분리합니다.
 *
 * [필요한 주입 객체]
 * 1. UserRepository userRepository : DB 사용자 조회/저장
 * 2. PasswordEncoder passwordEncoder : 비밀번호 암호화/비교
 * 3. EmailVerificationTokenRepository tokenRepository : 이메일 인증 토큰 저장/조회
 * 4. PasswordResetTokenRepository resetTokenRepository : 비밀번호 재설정 토큰 저장/조회
 *
 * [세션 기반 로그인 처리 로직]
 * - 카카오 로그인은 처음에 JWT 기반으로 유저 식별자/이메일을 받아옵니다.
 * - 서버는 이 정보를 믿고(또는 카카오 API 검증) DB에 해당 유저를 저장/로드합니다.
 * - 이후 해당 유저의 정보로 UsernamePasswordAuthenticationToken을 강제 생성하여
 * SecurityContextHolder.getContext().setAuthentication(인증객체) 를 호출합니다.
 * - 이로써 카카오 유저도 일반 사용자처럼 JSESSIONID(혹은 Spring Session) 기반의 세션 쿠키를 부여받게 됩니다.
 *
 * [구현해야 할 메서드]
 *
 * 1. login(LoginRequestDto request) → AuthResponseDto
 * - userRepository.findByEmail(request.getEmail()) 으로 사용자 조회
 * - passwordEncoder.matches(request.getPassword(), user.getPassword()) 비교
 * - 일치: 토큰 생성 (UUID 기반 등) + AuthResponseDto 반환
 * - 불일치: RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다")
 *
 * 2. signup(SignupRequestDto request) → AuthResponseDto
 * - 이메일 중복 검사: userRepository.existsByEmail(request.getEmail())
 * - User 엔티티 생성: password는 passwordEncoder.encode() 로 암호화
 * - provider = "LOCAL" 설정
 * - userRepository.save(user)
 * - 토큰 생성 + AuthResponseDto 반환
 *
 * 3. saveVerificationToken(String email, String code)
 * - EmailVerificationToken 빌더 패턴으로 생성
 * - expiresAt = LocalDateTime.now().plusMinutes(5)
 * - tokenRepository.save()
 *
 * 4. verifyEmailCode(String email, String code) → boolean
 * - tokenRepository.findByEmailAndCodeAndUsedFalse(email, code) 조회
 * - expiresAt > now 확인 → used = true 업데이트 → return true
 *
 * 5. resetPassword(String email, String newPassword)
 * - userRepository.findByEmail(email) 조회
 * - user.setPassword(passwordEncoder.encode(newPassword))
 * - userRepository.save(user)
 *
 * [사용 어노테이션]
 * - @Service : 서비스 빈 등록
 * - @RequiredArgsConstructor : final 필드 생성자 주입
 * - @Transactional : 메서드 단위 트랜잭션 관리 (DB 변경이 있는 메서드에 선언)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	@PersistenceContext
	private EntityManager em;
	private final EmailService emailService;
	private final EmailVerificationTokenRepository tokenRepository;
	private final PasswordResetTokenRepository resetTokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;


	// 1. login(LoginRequestDto request) → AuthResponseDto
	// - userRepository.findByEmail(request.getEmail()) 으로 사용자 조회
	// - passwordEncoder.matches(request.getPassword(), user.getPassword()) 비교
	// - 일치: 토큰 생성 (UUID 기반 등) + AuthResponseDto 반환
	// - 불일치: RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다")
	// [로그인] 회원여부 및 비번 조회
	@Transactional(readOnly = false)
	public AuthResponseDto login(String email, String password) {
		
		// <회원 여부 조회> : 엔티티에서 이메일 조회
		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다"));
		// <비번 검증> : 평문 비번과 db암호화 비번 비교
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new RuntimeException("비밀번호가 일치하지 않습니다");
		}
		;
		// 리프레쉬 토큰 생성및 db관리
		Long userId = user.getId(); 
		String userEmail = user.getEmail();
		
		String refreshTokenValue = jwtUtil.createRefreshToken(userId, userEmail);
		String accessToken = jwtUtil.createToken(user.getId(), user.getEmail());
		LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
		
		// 기존 유저의 리프레쉬 토큰 존애 여부
		refreshTokenRepository.findByUserId(user.getId())
			.ifPresentOrElse(
					//이미 존재하는 경우 업데이트
					existingToken -> {
						System.out.println("기존 토큰 업데이트 실행"); // 로그 확인용
		                existingToken.update(refreshTokenValue, expiryDate);
		                
		             // 핵심: 변경된 엔티티를 명시적으로 저장하고 즉시 반영(flush)
		                refreshTokenRepository.save(existingToken);
		                refreshTokenRepository.flush();
					}, 
					// 없는 경우
					() -> {
						System.out.println("신규 토큰 생성 실행"); 
						//새 리프레쉬 토큰 객체 생성시 유저를 넣어줌
						RefreshToken refreshToken = RefreshToken.builder()
						        .user(user)  // 여기서 조회한 유저 객체를 전달!
						        .token(refreshTokenValue)
						        .expiryDate(LocalDateTime.now().plusDays(7))
						        .build();

						refreshTokenRepository.save(refreshToken);
						refreshTokenRepository.flush();
						
					});
		
		
		// <DTO변환>
		UserProfileDto userProfileDto = UserProfileDto.from(user);
				
		// <결과 반환>
		String token = jwtUtil.createToken(user.getId(),user.getEmail());
		AuthResponseDto response =  AuthResponseDto.builder()
				.token(token)
				.refreshToken(refreshTokenValue)
				.user(UserProfileDto.from(user))
				.build();
		
		return response;
	}
/*
	// ✅ [새로운 메서드] 리프레시 토큰 저장/업데이트 로직 분리
		@Transactional(readOnly = false)
		 private void saveOrUpdateRefreshTokenInTransaction(UserEntity user, String refreshTokenValue, LocalDateTime expiryDate2) {
	        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
	        
	        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(user.getId());
	        
	        if (existingToken.isPresent()) {
	            // 기존 토큰 업데이트
	            RefreshToken token = existingToken.get();
	            token.update(refreshTokenValue, expiryDate);
	            // ✅ saveAndFlush() 사용!
	            refreshTokenRepository.saveAndFlush(token);
	            System.out.println("✅ 기존 리프레시 토큰 업데이트: User " + user.getId());
	        } else {
	            // 신규 토큰 생성
	            RefreshToken newToken = RefreshToken.builder()
	                    .user(user)
	                    .token(refreshTokenValue)
	                    .expiryDate(expiryDate)
	                    .build();
	            // ✅ saveAndFlush() 사용!
	            refreshTokenRepository.saveAndFlush(newToken);
	            System.out.println("✅ 신규 리프레시 토큰 생성: User " + user.getId());
	        }
	    }
*/
	// [회원 인증 여부 판단]
	public Boolean isAuthenticated(String email, String inputPassword) {
		// 이메일로 사용자만 먼저 조회
		return userRepository.findByEmail(email)
				.map(user -> {
					// db의 암호화된 비번과 평문 비번 비교
					return passwordEncoder.matches(inputPassword, user.getPassword());
				}).orElse(false);// user가 없으면 false 반환
	}

	// 이메일을 기반으로 사용자의 프로필 정보(DTO)를 조회
	public UserProfileDto getUserProfile(String email) {
		// 1. DB에서 유저 엔티티를 찾습니다.
		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수가 없습니다"));
		// 2. 엔티티를 DTO로 변환하여 반환합니다.
		return UserProfileDto.builder()
				.id(user.getId())
				.email(user.getEmail())
				.username(user.getUsername())
				.profileImageUrl(user.getProfileImageUrl())
				.statusMessage(user.getStatusMessage())
				.build();
	}

	// 2. signup(SignupRequestDto request) → AuthResponseDto

	@Transactional
	public Map<String, String> insertAuthDto(@Valid SignupRequestDto requestDto) {
		// 이메일 중복 여부 체크
		boolean isUser = userRepository.existsByEmail(requestDto.getEmail());
		if (isUser)
			return null;

		// 엔터티 저장
		UserEntity user = UserEntity
				.builder()
				.username(requestDto.getUsername())
				.password(passwordEncoder.encode(requestDto.getPassword()))
				.email(requestDto.getEmail())
				.provider("EMAIL")
				.createdAt(LocalDateTime.now()) // ← 수동으로 시간 주입
				.visibility(requestDto.getVisibility() != null ? requestDto.getVisibility() : "PUBLIC")
				.build();
		UserEntity insertedUser = userRepository.save(user);
		em.flush();

		// Map으로 변환해서 반환
		Map<String, String> map = new HashMap<>();
		map.put("message", "신규 회원 가입을 환영합니다.");
		map.put("username", requestDto.getUsername());
		map.put("email", requestDto.getEmail());
		return map;
	};


	@Transactional
	public void saveVerificationToken(String email, String code) {
		EmailVerificationToken token = new EmailVerificationToken();
		token.setEmail(email);
		token.setCode(code);
		token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
		tokenRepository.save(token);

		emailService.sendVerificationCode(email, code);
	}

	@Transactional
	public boolean verifyEmailCode(String email, String code) {
		EmailVerificationToken token = tokenRepository
				.findByEmailAndCodeAndUsedAtIsNull(email, code)
				.orElse(null);

		if (token == null)
			return false;
		if (token.getExpiresAt().isBefore(LocalDateTime.now()))
			return false;

		token.setUsed(true); // usedAt = now로 세팅됨
		tokenRepository.save(token);
		return true;
	}

	@Transactional
	public boolean verifyResetCode(String email, String code) {
		// PasswordResetToken에서 tokenHash(=code)로 조회
		PasswordResetToken token = resetTokenRepository
				.findByTokenHashAndUsedAtIsNull(code)
				.orElse(null);

		if (token == null)
			return false;
		// 해당 토큰의 사용자 이메일과 입력된 이메일이 일치하는지 확인
		if (!token.getUser().getEmail().equals(email))
			return false;
		if (token.getExpiresAt().isBefore(LocalDateTime.now()))
			return false;

		return true;
	}

	@Transactional
	public void resetPassword(String email, String newPassword) {
		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수가 없습니다"));

		PasswordResetToken token = resetTokenRepository
				.findTopByUserOrderByCreatedAtDesc(user)
				.orElseThrow(() -> new IllegalArgumentException("비밀번호 재설정 토큰이 없습니다"));

		if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("비밀번호 재설정 링크가 만료되었습니다.");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);

		token.setUsed(true);
		resetTokenRepository.save(token);
	}
	
	@Transactional
	public Map<String, Object> refreshAccessToken(String refreshTokenValue) {
		// db에서 해당 리프레쉬 토큰 찾기
		RefreshToken dbToken = refreshTokenRepository.findByToken(refreshTokenValue)
				.orElseThrow(()-> new RuntimeException("유효하지 않은 리프레시 토큰입니다."));
		
		//  만료 체크
	    if (dbToken.getExpiryDate().isBefore(LocalDateTime.now())) {
	        refreshTokenRepository.delete(dbToken);
	        throw new RuntimeException("리프레시 토큰이 만료되었습니다.");
	    }

	    //  새로운 토큰들 생성
	    UserEntity user = dbToken.getUser();
	    String newAccessToken = jwtUtil.createToken(user.getId(), user.getEmail());
	    String newRefreshToken = jwtUtil.createRefreshToken(user.getId(), user.getEmail());

	    //  DB 업데이트 (Dirty Checking에 의해 자동 저장됨)
	    dbToken.update(newRefreshToken, LocalDateTime.now().plusDays(7));
	    refreshTokenRepository.saveAndFlush(dbToken);
	    
	    //  결과 반환
	    Map<String, Object> result = new HashMap<>();
	    result.put("accessToken", newAccessToken);
	    result.put("refreshToken", newRefreshToken);
	    result.put("user", UserProfileDto.from(user)); // 프론트엔드 setUser를 위해 추가
	    
		// 토큰이 유효한 경우 신규 access토큰 발급
	
		return result;
		
		
	}
}
