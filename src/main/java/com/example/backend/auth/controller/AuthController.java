package com.example.backend.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.example.backend.auth.dto.EmailRequestDto;
import com.example.backend.auth.dto.EmailVerifyDto;
import com.example.backend.auth.dto.KakaoLoginDto;
import com.example.backend.auth.dto.LoginRequestDto;
import com.example.backend.auth.dto.ResetPasswordDto;
import com.example.backend.auth.dto.SignupRequestDto;
import com.example.backend.auth.entity.PasswordResetToken;
import com.example.backend.auth.entity.RefreshToken;
import com.example.backend.auth.repository.PasswordResetTokenRepository;
import com.example.backend.auth.repository.RefreshTokenRepository;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.service.EmailService;
import com.example.backend.auth.service.KakaoService;
import com.example.backend.global.config.JwtUtil;
import com.example.backend.user.dto.UserProfileDto;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.user.service.UserService;


/**
 * [인증 컨트롤러 (Auth Controller)] - 프론트엔드의 /api/auth/* 로 들어오는 요청을 처리합니다. -
 * 로그인/회원가입/로그아웃, 이메일 인증, 카카오 로그인(JWT 검증 후 세션 전환)을 담당합니다.
 *
 * [프론트엔드 authService.js와의 매핑]
 * ┌──────────────────────────────────────────────────────────────┐ │ 프론트 함수 │
 * 백엔드 엔드포인트 │ ├──────────────────────────┼───────────────────────────────────┤
 * │ authService.login() │ POST /api/auth/login │ │ authService.signup() │ POST
 * /api/auth/signup │ │ authService.logout() │ POST /api/auth/logout │ │
 * authService.sendEmailCode() │ POST /api/auth/email/send-code │ │
 * authService.sendResetEmailCode()│ POST /api/auth/email/send-reset-code │ │
 * authService.verifyEmailCode() │ POST /api/auth/email/verify-code │ │
 * authService.resetPassword() │ POST /api/auth/reset-password │ │
 * KakaoCallback.jsx │ POST /api/auth/kakao/login │
 * └──────────────────────────┴───────────────────────────────────┘
 *
 * [필요한 주입 서비스] - AuthService authService : 일반 로그인/회원가입/로그아웃 처리 - EmailService
 * emailService : 이메일 인증 코드 발송 - KakaoService kakaoService : 카카오 OAuth 처리
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController implements AuthControllerDocs {

	private final UserService userService;
	private final AuthService authService;
	private final EmailService emailService;
	private final KakaoService kakaoService;
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtUtil jwtUtil;

	/**
	 * [1] 로그인 — POST /api/auth/login
	 */
	@PostMapping("/login")
	@Transactional
	public ResponseEntity<?> login(@RequestBody LoginRequestDto credentials, HttpServletResponse response) {
	    // 1. 서비스 호출 및 인증 확인
	    Boolean isLogin = authService.isAuthenticated(credentials.getEmail(), credentials.getPassword());

	    if (isLogin) {
	        // 유저 정보 가져오기
	        UserProfileDto userProfile = authService.getUserProfile(credentials.getEmail());
	        
	        // 토큰 생성
	        String accessToken = jwtUtil.createToken(userProfile.getId(), userProfile.getEmail());
	        String refreshToken = jwtUtil.createRefreshToken(userProfile.getId(), userProfile.getEmail());
	        
	        // 2. Refresh Token DB 저장 (Optional.get() 보단 객체 존재 여부 확인 권장)
	        UserEntity user = userRepository.findById(userProfile.getId())
	                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

	        refreshTokenRepository.findByUserId(user.getId())
	                .ifPresentOrElse(
	                    existingToken -> existingToken.update(refreshToken, LocalDateTime.now().plusDays(7)),
	                    () -> {
	                        RefreshToken newRefreshToken = new RefreshToken(user, refreshToken, LocalDateTime.now().plusDays(7));
	                        refreshTokenRepository.save(newRefreshToken);
	                    }
	                );
	        
	        // 3. Refresh Token을 HttpOnly 쿠키로 설정
	        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
	                .httpOnly(true)
	                .secure(false) // 배포 시(HTTPS) true로 변경 권장
	                .path("/")
	                .maxAge(7 * 24 * 60 * 60)
	                .sameSite("Lax")
	                .build();

	        // 4. 프론트엔드 전달용 데이터 구성
	        Map<String, Object> responseBody = new HashMap<>();
	        responseBody.put("accessToken", accessToken);
	        responseBody.put("user", userProfile); // 이미 authService에서 빌더로 생성된 profile 활용 가능

		    return ResponseEntity.ok()
		    		.header("Set-Cookie", refreshTokenCookie.toString()) // 쿠키 설정
		    		.header("Authorization", "Bearer " + accessToken) // 액세스 토큰 헤더
		    		.body(responseBody);
	    } else {
	        // 5. 로그인 실패: 401 Unauthorized 반환
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일 또는 비밀번호가 일치하지 않습니다.");
	    }
	}


	/**
	 * [2] 회원가입 — POST /api/auth/signup
	 */
	@PostMapping("/signup")
	@Transactional
	public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequestDto requestDto) {
		// 서비스 호출
		// 1. DTO로 받기
		Map<String, String> response = authService.insertAuthDto(requestDto);
		// 가입 실패(중복,에러 등)시
		if (response == null || response.containsKey("error")) {
			Map<String, String> failedMap = new HashMap<>();
			failedMap.put("isExisted", requestDto.getUsername() + "님은 이미 가입한 메일 주소입니다");
			// 409 Confl
			return ResponseEntity.status(HttpStatus.CONFLICT).body(failedMap);
		}
		// 가입 성공시 (201 Created)
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}////

	/**
	 * [3] 로그아웃 — POST /api/auth/logout
	 */

	@PostMapping("/logout")
	@Transactional
	public ResponseEntity<?> logout(
			@RequestHeader(value = "Authorization", required = false) String authHeader,
			HttpServletResponse response) {
		// 토큰 존재 여부 및 형식 체크
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {

			return ResponseEntity.badRequest()
					.body(Map.of("error", "유효한 인증 헤더가 필요합니다."));
		}

		String accessToken = authHeader.substring(7);

		// db에서 삭제(폐기)
		try {
			Long userId = jwtUtil.getUserIdFromToken(accessToken);
			refreshTokenRepository.deleteByUserId(userId);
			
			// 브라우저의 리프레쉬 토큰 삭제
			ResponseCookie deleteCookie = ResponseCookie.from("refreshToken","")
					.httpOnly(true)
					.secure(false)
					.path("/")
					.maxAge(0)
					.sameSite("Lax")
					.build();
			
			return ResponseEntity.ok()
					.header("Set-Cookie", deleteCookie.toString())
					.body("로그아웃 되었습니다.");

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
		}

	}

	/**
	 * [4] 카카오 로그인 — POST /api/auth/kakao/login
	 */
	@PostMapping("/kakao/login")
	@Transactional
	public ResponseEntity<?> kakaoLogin(@RequestBody KakaoLoginDto dto, HttpSession session) {
	    // 1. 서비스 호출 및 유저 정보/신규 여부 추출
	    Map<String, Object> loginResult = kakaoService.processKakaoLogin(dto.getKakaoId(), dto.getUsername());

	    UserEntity user = Optional.ofNullable((UserEntity) loginResult.get("user"))
	            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 로그인 인증 실패"));
	    
	    boolean isNewUser = (boolean) loginResult.get("isNewUser");

	    // 2. JWT 토큰 생성 (Access & Refresh)
	    String accessToken = jwtUtil.createToken(user.getId(), user.getEmail());
	    String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getEmail());

	    // 3. Refresh Token DB 저장 (로그인 유지 세션 관리)
	    refreshTokenRepository.findByUserId(user.getId())
	            .ifPresentOrElse(
	                existingToken -> existingToken.update(refreshToken, LocalDateTime.now().plusDays(7)),
	                () -> {
	                    RefreshToken newRefreshToken = new RefreshToken(user, refreshToken, LocalDateTime.now().plusDays(7));
	                    refreshTokenRepository.save(newRefreshToken);
	                }
	            );

	    // 4. Refresh Token을 HttpOnly 쿠키로 설정
	    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
	            .httpOnly(true)
	            .secure(false) // HTTPS 환경에서는 true로 변경 필요
	            .path("/")
	            .maxAge(7 * 24 * 60 * 60) // 7일
	            .sameSite("Lax")
	            .build();

	    // 5. 프론트엔드 전달용 UserProfileDto 생성
	    UserProfileDto userProfileDto = UserProfileDto.builder()
	            .id(user.getId())
	            .email(user.getEmail())
	            .profileImageUrl(user.getProfileImageUrl())
	            .build();

	    // 6. 응답 바디 구성 (accessToken 및 신규 가입 여부 포함)
	    Map<String, Object> responseBody = new HashMap<>();
	    responseBody.put("user", userProfileDto);
	    responseBody.put("isNewUser", isNewUser);
	    responseBody.put("accessToken", accessToken);

	    return ResponseEntity.ok()
	    		.header("Set-Cookie", refreshTokenCookie.toString()) // 쿠키 설정
	    		.header("Authorization", "Bearer " + accessToken) // 액세스 토큰 헤더
	    		.body(responseBody);
	}

	/**
	 * [5] 이메일 인증 코드 발송 — POST /api/auth/email/send-code
	 */
	@PostMapping("/email/send-code")
	@Transactional
	public ResponseEntity<?> sendEmailCode(@RequestBody EmailRequestDto requestDto) {
		String email = requestDto.getEmail();

		if (userRepository.existsByEmail(email)) {
			return ResponseEntity.badRequest().body("이미 가입된 이메일입니다.");
		}

		String code = emailService.generateCode();
		authService.saveVerificationToken(email, code);

		return ResponseEntity.ok("인증 코드가 발송 되었습니다.");
	}

	/**
	 * [6] 비밀번호 재설정 인증 코드 발송 — POST /api/auth/email/send-reset-code
	 */

	@PostMapping("/email/send-reset-code")
	@Transactional
	public ResponseEntity<?> sendResetEmailCode(@RequestBody EmailRequestDto requestDto) {
		String email = requestDto.getEmail();
		Optional<UserEntity> optionalUser = userRepository.findByEmail(email);

		// 보안상 동일 응답
		if (optionalUser.isEmpty()) {
			return ResponseEntity.ok("이메일을 발송했습니다");
		}

		UserEntity user = optionalUser.get();
		String code = emailService.generateCode();

		PasswordResetToken token = PasswordResetToken.builder()
				.user(user)
				.tokenHash(code)
				.expiresAt(LocalDateTime.now().plusMinutes(5))
				.build();

		passwordResetTokenRepository.save(token);
		emailService.sendPasswordResetCode(email, code);

		return ResponseEntity.ok("이메일을 발송했습니다");
	}

	/**
	 * [7] 이메일 인증 코드 검증 — POST /api/auth/email/verify-code
	 */
	@PostMapping("/email/verify-code")
	@Transactional
	public ResponseEntity<?> verifyEmailCode(@RequestBody EmailVerifyDto requestDto) {
		boolean isAuth = authService.verifyEmailCode(requestDto.getEmail(), requestDto.getCode());
		if (!isAuth)
			return ResponseEntity.badRequest().body("인증 실패");
		return ResponseEntity.ok("인증 성공");
	}

	/**
	 * [8] 비밀번호 재설정 — POST /api/auth/reset-password
	 */
	@PostMapping("/reset-password")
	@Transactional
	public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request) {
		authService.resetPassword(request.getEmail(), request.getNewPassword());
		return ResponseEntity.ok("비밀번호가 변경되었습니다.");
	}

	/**
	 * [9] 인증코드 검증 — POST /api/email/verify-reset-code
	 */
	@PostMapping("/email/verify-reset-code")
	@Transactional
	public ResponseEntity<?> verifyResetCode(@RequestBody EmailVerifyDto requestDto) {
		boolean isAuth = authService.verifyResetCode(requestDto.getEmail(), requestDto.getCode());
		if (!isAuth)
			return ResponseEntity.badRequest().body("인증 실패");
		return ResponseEntity.ok("인증 성공");
	}

	/**
	 * [10] 이메일 중복여부 체크 — POST /api/email/check
	 */
	@GetMapping("/email/check")
	@Transactional
	public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
		boolean isDuplicate = userRepository.existsByEmail(email);
		return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
	}

	/**
	 * [11] Access Token 재발급 — POST /api/auth/refresh
	 */
	@PostMapping("/refresh")
	@Transactional
	public ResponseEntity<?> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {

	    // 1. refreshToken 존재 여부 확인
	    if (refreshToken == null || refreshToken.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                .body(Map.of("error", "Refresh Token이 필요합니다"));
	    }

	    // 2. 토큰 유효성 및 타입 확인
	    if (!jwtUtil.validationToken(refreshToken)) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(Map.of("error", "유효하지 않은 Refresh Token입니다"));
	    }
	    
	    if (!jwtUtil.isRefreshToken(refreshToken)) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(Map.of("error", "Access Token이 아닌 Refresh Token이 필요합니다."));
	    }

	    try {
	        // 3. DB에서 토큰 검증
	        RefreshToken dbToken = refreshTokenRepository.findByToken(refreshToken)
	                .orElse(null);

	        if (dbToken == null) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body(Map.of("error", "존재하지 않는 리프레시 토큰입니다. 다시 로그인하세요."));
	        }

	        // 4. 기간 만료 확인
	        if (dbToken.getExpiryDate().isBefore(LocalDateTime.now())) {
	            refreshTokenRepository.delete(dbToken);
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body(Map.of("error", "리프레시 토큰이 만료되었습니다."));
	        }

	        // 5. 새로운 토큰 정보 추출 및 생성 (Rotation 방식)
	        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
	        String email = jwtUtil.getUserEmailFromToken(refreshToken);

	        String newAccessToken = jwtUtil.createToken(userId, email);
	        String newRefreshToken = jwtUtil.createRefreshToken(userId, email);

	        // 6. DB 갱신 (기존 토큰 레코드 업데이트)
	        dbToken.update(newRefreshToken, LocalDateTime.now().plusDays(7));
	        refreshTokenRepository.save(dbToken);

	        // 7. 새 리프레시 토큰을 쿠키에 설정
	        ResponseCookie newCookie = ResponseCookie.from("refreshToken", newRefreshToken)
	                .httpOnly(true)
	                .secure(false)
	                .path("/")
	                .maxAge(7 * 24 * 60 * 60)
	                .sameSite("Lax")
	                .build();

	        return ResponseEntity.ok()
	        		.header("Set-Cookie", newCookie.toString())
	        		.body(Map.of("accessToken", newAccessToken));

	    } catch (Exception e) {
	    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	        		.body(Map.of("error", "서버 오류가 발생했습니다: " + e.getMessage()));
	}


	}

	/**
	 * [12] Token 정보 조회 — GET /api/auth/token-info
	 */
	@GetMapping("/token-info")
	@Transactional 
	public ResponseEntity<?> getToken(Authentication authentication) {
			// 필터에서 저장한 userId 추출
		try {
			Long userId = (Long)authentication.getPrincipal();

			Map<String, Object> response = new LinkedHashMap<>();
			response.put("userId", userId);
			response.put("statue", "AUTHENTICATED");

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Token 정보 조회 중 오류가 발생했습니다."));
		}

	}

}
