package com.example.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.backend.user.entity.UserEntity; //(본인의 User 엔티티 경로 확인)

import com.example.backend.auth.entity.RefreshToken;
import com.example.backend.auth.repository.RefreshTokenRepository;
import com.example.backend.auth.service.KakaoService;

import org.springframework.http.ResponseCookie;

import lombok.RequiredArgsConstructor;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // 모든 final 필드를 포함하는 생성자를 자동으로 생성합니다.
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JwtUtil jwtUtil;
    private final KakaoService kakaoService;
    private final RefreshTokenRepository refreshTokenRepository;

    // 수동 생성자는 삭제하거나 모든 필드를 포함해야 합니다.
    // @RequiredArgsConstructor가 있으므로 아래 생성자는 지우셔도 됩니다.

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/login/**",
                                "/oauth2/**",
                                "/login/oauth2/code/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/login/**",
                                "/error",
                                "/fss/**",
                                "/friends/**",
                                //"/users/**",
                                "/users/me",
                                "/kakao/login",
                                "/badges/ranking/**",
                                "/photos/**",
                                "/albums/**",
                                "/uploads/**",
                                "/qna/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JWTAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository())
                                .authorizationRequestResolver(authorizationRequestResolver()))
                        // .redirectionEndpoint(redir -> redir
                        // .baseUri("/login/oauth2/code/*"))
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler(
                                "http://localhost:5173/login?error=true")));

        return http.build();
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        System.out.println("핸들러 진입 성공");
        return (request, response, authentication) -> {
            try {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                Map<String, Object> attributes = oAuth2User.getAttributes();

                // 1. 카카오 정보 추출
                Long kakaoId = Long.parseLong(attributes.get("id").toString());

                Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
                String nickname = "Unknown";
                if (properties != null && properties.get("nickname") != null) {
                    nickname = properties.get("nickname").toString();
                }

                // 2. ★ DB 저장 로직 호출 ★
                // 이제 Map 형태로 user와 isNewUser를 받아옵니다.
                Map<String, Object> loginResult = kakaoService.processKakaoLogin(kakaoId, nickname);
                if (loginResult == null || !loginResult.containsKey("user") || !loginResult.containsKey("isNewUser")) {
                    throw new IllegalStateException("로그인 처리 중 오류가 발생했습니다.");
                }
                UserEntity user = (UserEntity) loginResult.get("user");
                boolean isNewUser = (boolean) loginResult.get("isNewUser");

                System.out.println("=== DB 저장/조회 완료 (신규여부: " + isNewUser + ") ===");

                // 3. JWT 토큰 생성
                String accessToken = jwtUtil.createToken(user.getId(), user.getEmail());
                String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getEmail());

                // 4. Refresh Token DB 저장
                refreshTokenRepository.findByUserId(user.getId())
                        .ifPresentOrElse(
                            existingToken -> existingToken.update(refreshToken, LocalDateTime.now().plusDays(7)),
                            () -> {
                                RefreshToken newRefreshToken = new RefreshToken(user, refreshToken, LocalDateTime.now().plusDays(7));
                                refreshTokenRepository.save(newRefreshToken);
                            }
                        );

                // 5. Refresh Token을 HttpOnly 쿠키로 설정
                ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(7 * 24 * 60 * 60)
                        .sameSite("Lax")
                        .build();
                response.addHeader("Set-Cookie", refreshTokenCookie.toString());

                // 6. 프론트엔드 콜백 URL로 리다이렉트 (refreshToken은 쿠키로 전달)
                UriComponentsBuilder uriBuilder = UriComponentsBuilder
                        .fromUriString("http://localhost:5173/auth/kakao/callback")
                        .queryParam("accessToken", accessToken)
                        .queryParam("isNewUser", isNewUser); // 신규 가입 여부 전달

                if (isNewUser) {
                    // 신규 가입자라면 닉네임도 같이 보내서 환영 문구에 사용
                    uriBuilder.queryParam("nickname", URLEncoder.encode(nickname, "UTF-8"));
                }

                String targetUrl = uriBuilder.build().toUriString();
                response.sendRedirect(targetUrl);

            } catch (Exception e) {
                System.out.println("=== SuccessHandler 에러 발생 ===");
                e.printStackTrace();
                response.sendRedirect("http://localhost:5173/login?error=handler");
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://192.168.0.44:5173",
                "http://10.0.2.2:8080",
                "http://localhost",
                "capacitor://localhost"));

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        config.setAllowedHeaders(Arrays.asList(
                "Authorization", // JWT Bearer 토큰
                "Content-Type", // application/json
                "X-Requested-With", // Ajax 요청 식별
                "Accept",
                "Origin")); 

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer -> customizer.attributes(attrs -> {
            attrs.remove("code_challenge");
            attrs.remove("code_challenge_method");
        })
                .additionalParameters(params -> {
                    params.remove("code_challenge");
                    params.remove("code_challenge_method");

                    // 카카오에게 매번 로그인 창을 띄우라고 명령 ★
                    // "login": 로그인 폼 출력 / "consent": 동의 화면 출력
                    params.put("prompt", "login");
                }));
        return resolver;
    }
}