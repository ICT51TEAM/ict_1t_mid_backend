package com.example.backend.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * [카카오 로그인 서비스 (Kakao Service)]
 * - 카카오 OAuth 인가 코드를 받아 → 카카오 토큰 발급 → 사용자 정보 조회 → 자동 회원생성/로그인
 * - JWT 방식으로 처음에만 카카오에서 사용자 정보를 받아오고,
 * 로그인 성공 이후에는 일반 로그인과 동일하게 세션 방식으로 관리
 *
 * [수정사항]
 * - 신규 가입 여부(isNewUser)를 판단하여 AuthController에 전달할 수 있도록 Map 구조 반환 로직 추가
 */
@Service
@RequiredArgsConstructor
public class KakaoService {
	
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private static final Logger log = LoggerFactory.getLogger(KakaoService.class);
	
    /**
     * 기존 회원 여부 확인만 수행
     */
    public Optional<UserEntity> findKakaoUser(String kakaoId) {
        return userRepository.findByOauthProviderIdAndProvider(kakaoId, "KAKAO");
    }
	
    /**
     * 카카오 로그인 및 회원가입 통합 처리
     * @return Map (user: UserEntity, isNewUser: Boolean)
     */
    @Transactional
    public Map<String, Object> processKakaoLogin(Long kakaoId, String username) {
        log.info(">>>> [DEBUG] processKakaoLogin 진입 성공! kakaoId: {}", kakaoId);
        
        String provider = "KAKAO";
        String strKakaoId = String.valueOf(kakaoId);
        Map<String, Object> result = new HashMap<>();
        
        // 1. 기존 회원 여부 판단 (카카오 공급자 ID와 제공처 비교)
        Optional<UserEntity> userOpt = userRepository.findByOauthProviderIdAndProvider(strKakaoId, provider);

        if (userOpt.isPresent()) {
            // [기존 회원인 경우]
            log.info("2-1. 기존 회원 발견: {}", userOpt.get().getEmail());
            result.put("user", userOpt.get());
            result.put("isNewUser", false); // 기존 회원이므로 false
        } else {
            // [신규 회원인 경우]
            log.info("2-2. 카카오 신규 회원 가입 로직 진입");
            UserEntity newUser = registerNewUser(strKakaoId, provider, username);
            result.put("user", newUser);
            result.put("isNewUser", true); // 신규 가입이므로 true
        }
        
        return result;
    }
		
    /**
     * 신규 사용자 등록 (DB 저장)
     */
    private UserEntity registerNewUser(String kakaoId, String provider, String nickname) {
        log.info("3. registerNewUser 호출됨 - DB 저장 시도");
        
        // 요구사항 3 & 4용 고유 문자열 생성: "카카오ID_KAKAO"
        String generatedValue = kakaoId + "_" + provider;

        // 요구사항 4: 생성된 문자열을 암호화 (패스워드 해싱)
        String encodedPassword = bCryptPasswordEncoder.encode(generatedValue);

        // 요구사항 2: 새로운 UserEntity 생성 및 데이터 세팅
        UserEntity newUser = new UserEntity();
        
        // 1번 컬럼 업데이트 (OAUTH_PROVIDER_ID)
        newUser.setOauthProviderId(kakaoId); 
        
        // 요구사항 2: OAUTH_PROVIDER에 KAKAO 추가
        newUser.setProvider(provider); 
        
        // 요구사항 3: EMAIL에 "ID_KAKAO" 추가
        newUser.setEmail(generatedValue); 
        
        // 요구사항 4: PASSWORD_HASH에 암호화된 값 추가
        newUser.setPassword(encodedPassword); 
        
        // 필수값인 username 및 기본 설정
        newUser.setUsername(nickname);
        newUser.setVisibility("PUBLIC");
        
        // 🚨 [에러 해결] 날짜 정보 수동 주입 (ORA-01400 방지)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        newUser.setCreatedAt(now); 
        newUser.setUpdatedAt(now);

        UserEntity savedUser = userRepository.save(newUser);
        log.info("4. DB 저장 완료 - 생성된 ID: {}", savedUser.getId());
        
        return savedUser;
    }
}