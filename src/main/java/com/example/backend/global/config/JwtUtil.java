package com.example.backend.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.id.JWTID;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {

	// JWT의 Payload(내용) 부분에 사용자가 원하는 커스텀 정보를 담는 것을 **Claim(클레임)**이라고 한다.
	// 검증용 비밀키 및 토큰 유효시간 받아오기
	private final SecretKey key;// JWT 서명/검증용 비밀키
	private final long jwtExpirationMs;// 토큰 유효 시간(ms)
	private final long refreshTokenExpirationMs = 7 * 24 * 60 * 60 * 1000;

	public JwtUtil(@Value("${jwt.secret}") String secretKey,
			@Value("${jwt.expiration-ms}") long jwtExpirationMs) {

		System.out.println("=== JWT Secret 앞 10자: " + secretKey.substring(0, Math.min(10, secretKey.length())));

		// ※Base64 인코딩된 키 생성
		// ※환경변수 등록키와 연동
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.jwtExpirationMs = jwtExpirationMs;
	}////////////////////

	// 2. 문자열 키를 Key 객체로 변환
	public String createToken(long userId, String email) {
		return Jwts.builder()
				.setSubject(email)
				.claim("userId", userId)
				.claim("type", "ACCESS")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				// 3. SignatureAlgorithm과 Key 객체를 함께 전달 (무결성 검증용)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	// 토큰에서 userId추출
	public Long getUserIdFromToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody()
				.get("userId", Long.class);
	}

	// 토큰에서 email추출
	public String getUserEmailFromToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();
	}

	// 토큰 유효성 체크하기
	public boolean validationToken(String token) {
		try {
			// 서버 인증 key값 수정여부 체크
			Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	// refreshToken 생성
	public String createRefreshToken(long userId, String email) {
		return Jwts.builder()
				.setSubject(email)
				.claim("userId", userId)
				.claim("type", "REFRESH")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();

	}

	// 토큰이 refreshToken 인지 확인
	public boolean isRefreshToken(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token)
					.getBody();
			return "REFRESH".equals(claims.get("type", String.class));
		} catch (Exception e) {
			return false;
		}
	}

	// 토큰이 AccessToken인지 확인
	public boolean isAccessToken(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token)
					.getBody();
			return "ACCESS".equals(claims.get("type", String.class));
		} catch (Exception e) {
			return false;
		}
	}

	// 토큰의 만료 시간까지 남은 시간(ms) 반환
	public long getExpirationTime(String token) {
		Date expiration = Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody()
				.getExpiration();
		return expiration.getTime() - System.currentTimeMillis();
	}

	// 토큰이 만료되었는지 확인
	public boolean isTokenExpired(String token) {
		try {
			Date expiration = Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token)
					.getBody()
					.getExpiration();
			return expiration.before(new Date());
		} catch (Exception e) {
			return true; // 파싱 실패 = 만료됨
		}
	}

	// 토큰의 모든 Claims 반환
	public Claims getAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

}
