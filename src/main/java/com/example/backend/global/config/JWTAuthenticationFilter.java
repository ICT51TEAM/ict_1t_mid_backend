package com.example.backend.global.config;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.openid.connect.sdk.federation.utils.JWTUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 토큰 검증 로직을 활용해 토큰에서 uerId를 추출하는 필터 역할의 클래스임
@Slf4j
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

	// 토큰 제공자 주입
	private final JwtUtil jwtUtil;

	@Override
	public void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain)
			throws IOException, ServletException {

		// 1. 헤더에서 토큰 추출
		String authHeader = request.getHeader("Authorization");
		String token = null;

		log.info("=== 요청 URL: {}, Authorization 헤더: {}", request.getRequestURI(),
				authHeader != null ? "있음(길이:" + authHeader.length() + ")" : "없음");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		}

		// 2. 추출된 토큰 유효성 검증
		if (token != null) {
			try {
				if (jwtUtil.validationToken(token)) {
					// 3. 토큰에서 userId 추출
					Long userId = jwtUtil.getUserIdFromToken(token);
					// 4. Spring Security 인증 객체 생성(password는 null, 권한은 비어있음)
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId,
							null, Collections.emptyList());
					// 5. 시큐리티 컨텍스트에 인증 정보 저장
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.info("인증 성공:유저 Id{}", userId);
				}
			} catch (ExpiredJwtException e) {
				log.error("토큰 만료됨:{}", e.getMessage());
				sendErrorResponse(response, "TOKEN_EXPIRED", "로그인 후 시간이 오래지났습니다. 다시 로그인해주세요.");
				return;
			} catch (Exception e) {
				log.error("유효하지 않은 토큰:{}", e.getMessage());
				sendErrorResponse(response, "INVALID_TOKEN", "인증에 실패했습니다. 다시 로그인해주세요.");
				return;
			}
		}
		// 6. 다음 필터 진행
		filterChain.doFilter(request, response);
	}/// doFilterInternal

	private void sendErrorResponse(HttpServletResponse response, String code, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json;charset=UTF-8");

		// 프론트로 보낼 에러 메세지 JSON
		String json = String.format("{\"code\":\"%s\", \"message\" : \"%s\"}", code, message);
		response.getWriter().write(json);

	}/// sendErrorResponse

}////// JWTAuthenticationFilter
