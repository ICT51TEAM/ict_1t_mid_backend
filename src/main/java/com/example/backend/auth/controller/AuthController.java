package com.example.backend.auth.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.example.backend.auth.dto.EmailRequestDto;
import com.example.backend.auth.dto.EmailVerifyDto;
import com.example.backend.auth.dto.KakaoLoginDto;
import com.example.backend.auth.dto.LoginRequestDto;
import com.example.backend.auth.dto.ResetPasswordDto;
import com.example.backend.auth.dto.SignupRequestDto;
import com.example.backend.auth.entity.PasswordResetToken;
import com.example.backend.auth.repository.PasswordResetTokenRepository;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.service.EmailService;
import com.example.backend.auth.service.KakaoService;
import com.example.backend.global.config.JwtUtil;
import com.example.backend.user.dto.UserProfileDto;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.user.service.UserService;
import com.nimbusds.oauth2.sdk.Request;

import lombok.RequiredArgsConstructor;

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
	private final JwtUtil jwtUtil;

	/**
	 * [1] 로그인 — POST /api/auth/login
	 * 
	 * @param request LoginRequestDto (email, password)
	 * @return AuthResponseDto (token, user)
	 * 
	 *         로직 힌트: - authService.login(request) 호출 - 내부: email로 DB 조회 → 비밀번호
	 *         BCrypt 비교 → 토큰 생성 → 세션/인증 설정 - 성공: ResponseEntity.ok(AuthResponseDto)
	 *         반환 - 실패: ResponseEntity.status(401).body("이메일 또는 비밀번호가 일치하지 않습니다")
	 */
	// [로그인]
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequestDto credentials, HttpSession session) {
		// 1. 서비스 호출 및 인증 확인
		Boolean isLogin = authService.isAuthenticated(credentials.getEmail(), credentials.getPassword());

		if (isLogin) {
			// 2. Spring Security 신분증(Authentication) 만들기
			Authentication authentication = new UsernamePasswordAuthenticationToken(
					credentials.getEmail(), null, new ArrayList<>());

			// 3. Security 금고에 신분증 넣기
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);

			// 4. 세션(HttpSession)에 이 금고 정보를 저장하기 (핵심!)
			session.setAttribute(
					HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
					SecurityContextHolder.getContext());

			// 5. 성공 시 프로필 정보 반환
			UserProfileDto userProfile = authService.getUserProfile(credentials.getEmail());
			String token = jwtUtil.createToken(userProfile.getId(), userProfile.getEmail());

			Map<String, Object> response = new HashMap<>();
			response.put("token", token);
			response.put("user", userProfile);
			return ResponseEntity.ok(response);
		} else {
			// 3. 로그인 실패: 401 Unauthorized 반환
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일 또는 비밀번호가 일치하지 않습니다.");
		}
	}

	/**
	 * [2] 회원가입 — POST /api/auth/signup
	 * 
	 * @param request SignupRequestDto (email, password, username)
	 * @return AuthResponseDto (token, user)
	 * 
	 *         로직 힌트: - 이메일 중복 검사 (userRepository.existsByEmail()) - 비밀번호 암호화
	 *         (passwordEncoder.encode()) - User 엔티티 생성 및 DB 저장 (provider = "LOCAL")
	 *         - 토큰 발급 → AuthResponseDto 반환
	 */
	@PostMapping("/signup")
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
	 * 
	 * @param request HttpServletRequest
	 * 
	 *                로직 힌트: - request.getSession().invalidate() 로 현재 세션 무효화 -
	 *                SecurityContextHolder.clearContext() 로 인증 정보 제거
	 */

	@PostMapping("/logout")
	public ResponseEntity<?> logout() {
		return ResponseEntity.ok("로그아웃 되었습니다.");
	}

	/**
	 * [4] 카카오 로그인 — POST /api/auth/kakao/login
	 * 
	 * @param request KakaoLoginDto (kakaoCode)
	 * @return AuthResponseDto (token, user)
	 * 
	 *         로직 힌트: - kakaoService.loginOrRegister(request.getKakaoCode()) 호출 -
	 *         내부: 인가코드 → 카카오 토큰 → 사용자정보 → DB 조회/생성 → 세션 발급
	 */
	@PostMapping("/kakao/login")
	public ResponseEntity<?> kakaoLogin(@RequestBody KakaoLoginDto dto, HttpSession session) {
		// 1. 서비스 호출 결과가 Map으로 변경됨
		Map<String, Object> loginResult = kakaoService.processKakaoLogin(dto.getKakaoId(), dto.getUsername());

		UserEntity user = (UserEntity) loginResult.get("user");
		boolean isNewUser = (boolean) loginResult.get("isNewUser"); // 신규 여부 추출

		// 2. 세션 인증 처리
		Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
				new ArrayList<>());
		SecurityContextHolder.getContext().setAuthentication(authentication);
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				SecurityContextHolder.getContext());

		// 3. 응답 객체 생성 (isNewUser 포함)
		Map<String, Object> response = new HashMap<>();
		response.put("user", user);
		response.put("isNewUser", isNewUser);

		return ResponseEntity.ok(response);
	}

	/**
	 * [5] 이메일 인증 코드 발송 — POST /api/auth/email/send-code
	 * 
	 * @param request EmailRequestDto (email)
	 * 
	 *                로직 힌트: - emailService.generateCode() 로 6자리 코드 생성 -
	 *                EmailVerificationToken 엔티티 저장 (email, code, expiresAt = now +
	 *                5분) - emailService.sendVerificationCode(email, code) 로 이메일 발송
	 */
	// @PostMapping("/email/send-code")
	// public ResponseEntity<?> sendEmailCode(@RequestBody EmailRequestDto request)
	// {
	// // 여기에 코드를 작성하세요.
	// }

	/**
	 * [6] 비밀번호 재설정 인증 코드 발송 — POST /api/auth/email/send-reset-code
	 * 
	 * @param request EmailRequestDto (email)
	 * 
	 *                로직 힌트: - 해당 이메일이 DB에 존재하는지 확인 - 존재하면 인증 코드 생성 →
	 *                PasswordResetToken 엔티티 저장 → 이메일 발송 - 미존재 시: 보안상 "이메일을 발송했습니다"
	 *                동일 응답 (이메일 존재 여부 노출 방지)
	 */
	// @PostMapping("/email/send-reset-code")
	// public ResponseEntity<?> sendResetEmailCode(@RequestBody EmailRequestDto
	// request) {
	// // 여기에 코드를 작성하세요.
	// }

	/**
	 * [7] 이메일 인증 코드 검증 — POST /api/auth/email/verify-code
	 * 
	 * @param request EmailVerifyDto (email, code)
	 * 
	 *                로직 힌트: - EmailVerificationTokenRepository에서
	 *                email+code+used=false 조회 - 존재 && expiresAt > now → 인증 성공, used
	 *                = true 업데이트 - 미존재 || 만료 → 인증 실패 응답
	 */
	// @PostMapping("/email/verify-code")
	// public ResponseEntity<?> verifyEmailCode(@RequestBody EmailVerifyDto request)
	// {
	// // 여기에 코드를 작성하세요.
	// }

	/**
	 * [8] 비밀번호 재설정 — POST /api/auth/reset-password
	 * 
	 * @param request ResetPasswordDto (email, newPassword)
	 * 
	 *                로직 힌트: - 이메일로 사용자 조회 - 비밀번호 암호화
	 *                (passwordEncoder.encode(newPassword)) - DB 업데이트
	 *                (user.setPassword(encodedPassword)) - 사용된 PasswordResetToken의
	 *                used = true 처리
	 */
	// @PostMapping("/reset-password")
	// public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request)
	// {
	// // 여기에 코드를 작성하세요.
	// }

	@PostMapping("/email/send-code")
	public ResponseEntity<?> sendEmailCode(@RequestBody EmailRequestDto requestDto) {
		String email = requestDto.getEmail();

		if (userRepository.existsByEmail(email)) {
			return ResponseEntity.badRequest().body("이미 가입된 이메일입니다.");
		}

		String code = emailService.generateCode();
		authService.saveVerificationToken(email, code);

		return ResponseEntity.ok("인증 코드가 발송 되었습니다.");
	}

	@PostMapping("/email/verify-code")
	public ResponseEntity<?> verifyEmailCode(@RequestBody EmailVerifyDto requestDto) {
		boolean isAuth = authService.verifyEmailCode(requestDto.getEmail(), requestDto.getCode());
		if (!isAuth)
			return ResponseEntity.badRequest().body("인증 실패");
		return ResponseEntity.ok("인증 성공");
	}

	@PostMapping("/email/send-reset-code")
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

	@PostMapping("/email/verify-reset-code")
	public ResponseEntity<?> verifyResetCode(@RequestBody EmailVerifyDto requestDto) {
		boolean isAuth = authService.verifyResetCode(requestDto.getEmail(), requestDto.getCode());
		if (!isAuth)
			return ResponseEntity.badRequest().body("인증 실패");
		return ResponseEntity.ok("인증 성공");
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request) {
		authService.resetPassword(request.getEmail(), request.getNewPassword());
		return ResponseEntity.ok("비밀번호가 변경되었습니다.");
	}
	
	// 이메일 중복여부 체크
	@GetMapping("/email/check")
	public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
	    boolean isDuplicate = userRepository.existsByEmail(email);
	    return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
	}

}
