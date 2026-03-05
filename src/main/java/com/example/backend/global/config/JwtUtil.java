package com.example.backend.global.config;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;


@Component
public class JwtUtil {
	
	//JWT의 Payload(내용) 부분에 사용자가 원하는 커스텀 정보를 담는 것을 **Claim(클레임)**이라고 한다.
	// 검증용 비밀키 및 토큰 유효시간 받아오기
	private final SecretKey key;//JWT 서명/검증용 비밀키	
	private final long jwtExpirationMs;//토큰 유효 시간(ms)
	
	public JwtUtil(@Value("${jwt.secret}") String secretKey,
					@Value("${jwt.expiration-ms}") long jwtExpirationMs) {
		
		//※Base64 인코딩된 키 생성
        //※환경변수 등록키와 연동
		String encodedBase64Key=Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
		
		this.key=Keys.hmacShaKeyFor(encodedBase64Key.getBytes());//서명용 SecretKey 객체 생성
		this.jwtExpirationMs=jwtExpirationMs;
	}////////////////////
	
	
    // 2. 문자열 키를 Key 객체로 변환
    public String createToken(long userId, String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
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
    			.get("userId",Long.class);		
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
	    	//서버 인증 key값 수정여부 체크
	    	Jwts.parserBuilder()
	    		.setSigningKey(key)
	    		.build()
	    		.parseClaimsJws(token);
	    	return true;
    }

}
