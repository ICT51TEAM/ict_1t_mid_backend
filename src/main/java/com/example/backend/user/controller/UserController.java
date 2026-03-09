package com.example.backend.user.controller;

import java.net.Authenticator;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.backend.auth.dto.LoginRequestDto;
import com.example.backend.global.config.JwtUtil;
import com.example.backend.user.dto.ChangePasswordDto;
import com.example.backend.user.dto.UpdateProfileDto;
import com.example.backend.user.dto.UserProfileDto;
import com.example.backend.user.dto.UserSettingsDto;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.user.service.UserService;
import com.example.backend.user.entity.UserEntity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * [사용자 컨트롤러 (User Controller)]
 * - 프론트엔드 userService.js의 /api/users/* 요청을 처리합니다.
 * - 프로필 조회/수정, 이미지 업로드, 비밀번호 변경, 회원탈퇴, 설정 관리
 *
 * [프론트엔드 userService.js와의 매핑]
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 프론트 함수 │ 백엔드 엔드포인트 │
 * ├──────────────────────────────┼───────────────────────────────┤
 * │ userService.getMyProfile() │ GET /api/users/me │
 * │ userService.getUserProfile() │ GET /api/users/{userId} │
 * │ userService.updateProfile() │ PUT /api/users/me │
 * │ userService.uploadProfileImage() │ POST /api/users/me/profile-image │
 * │ userService.changePassword() │ PUT /api/users/me/password │
 * │ userService.deleteAccount() │ DELETE /api/users/me │
 * │ userService.getSettings() │ GET /api/users/me/settings │
 * │ userService.updateSettings() │ PUT /api/users/me/settings │
 * └──────────────────────────────┴───────────────────────────────┘
 *
 * [필요한 주입 서비스]
 * - UserService userService
 *
 * [현재 로그인 사용자 확인 방법]
 * - @AuthenticationPrincipal UserDetails userDetails 파라미터 사용
 * - 또는 SecurityContextHolder.getContext().getAuthentication() 에서 추출
 * - 또는 커스텀 ViewerUserProvider 유틸 클래스 활용
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final BCryptPasswordEncoder passwordEncoder;

    // 서비스 주입
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * [1] 내 프로필 조회 — GET /api/users/me
     * 
     * @return UserProfileDto (id, email, username, profileImageUrl, statusMessage,
     *         provider, visibility)
     *
     *         로직 힌트:
     *         - 현재 로그인 사용자 ID 획득 (SecurityContext 또는 Authorization 헤더)
     *         - userService.getProfile(userId) 호출
     *         - User 엔티티 → UserProfileDto 변환 후 반환
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId) {

        // 서비스에서 DTO가져오기
        UserProfileDto myProfile = userService.getProfileById(userId);
        // 결과값 반환
        return ResponseEntity.ok(myProfile);
    }

    /**
     * [2] 특정 사용자 프로필 조회 — GET /api/users/{userId}
     * 
     * @param userId 조회할 사용자 ID
     * @return UserProfileDto
     *
     *         로직 힌트:
     *         - userService.getProfile(userId) 호출
     *         - 공개 범위(visibility) 확인 로직 필요 시 추가
     */
    // @GetMapping("/{userId}")
    // public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
    // // 여기에 코드를 작성하세요.
    // }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        // 서비스에서 DTO가져오기
        UserProfileDto userProfile = userService.getProfile(userId);
        // DTO 반환
        return ResponseEntity.ok(userProfile);
    }

    /**
     * [3] 프로필 수정 — PUT /api/users/me
     * 
     * @param request UpdateProfileDto (username, visibility)
     *
     *                로직 힌트:
     *                - 현재 로그인 사용자 조회
     *                - user.setUsername(request.getUsername())
     *                - user.setVisibility(request.getVisibility())
     *                - userRepository.save(user)
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestBody UpdateProfileDto request,
            @AuthenticationPrincipal Long userId) {
        // 서비스를 통해 DTO 업데이트
        UserProfileDto updateProfile = userService.updateProfile(userId, request);
        // 반환
        return ResponseEntity.ok(updateProfile);
    }

    /**
     * [4] 프로필 이미지 업로드 — POST /api/users/me/profile-image
     * 
     * @param file MultipartFile (이미지 파일)
     * @return { "profileImageUrl": "/uploads/profiles/xxx.jpg" }
     *
     *         로직 힌트:
     *         - MultipartFile을 서버 로컬 경로에 저장 (예: ./uploads/profiles/)
     *         - 파일명 중복 방지: UUID + 원본 확장자
     *         - user.setProfileImageUrl(저장된 경로)
     *         - DB 업데이트 후 이미지 URL 반환
     */
    @PostMapping("/me/profile-image")
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {
        String imageUrl = userService.uploadProfileImage(userId, file);
        return ResponseEntity.ok(Map.of("profileImageUrl", imageUrl));
    }
    /**
     * [5] 비밀번호 변경 — PUT /api/users/me/password
     * 
     * @param request ChangePasswordDto (currentPassword, newPassword)
     *
     *                로직 힌트:
     *                - 현재 비밀번호 검증: passwordEncoder.matches(currentPassword,
     *                user.getPassword())
     *                - 새 비밀번호 암호화: passwordEncoder.encode(newPassword)
     *                - user.setPassword(encodedNewPassword)
     *                - 카카오 유저(provider="KAKAO")는 비밀번호 변경 불가 처리 권장
     * @throws IllegalAccessException
     */
    // @PutMapping("/me/password")
    // public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto
    // request) {
    // // 여기에 코드를 작성하세요.
    // }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordDto request,
            @AuthenticationPrincipal Long userId) throws IllegalAccessException {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수가 없습니다"));
        // 현재 비번 일치 여부 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalAccessException("현재 비밀 번호가 일치하지 않습니다.");
        }
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }

    /**
     * [6] 회원 탈퇴 — DELETE /api/users/me
     * 
     * @param body { "password": "현재비밀번호" }
     *
     *             로직 힌트:
     *             - 비밀번호 검증 후 사용자 삭제
     *             - 연관된 데이터(게시글, 친구관계 등)도 함께 삭제 또는 비활성화 처리
     *             - 세션 무효화
     */
    // @DeleteMapping("/me")
    // public ResponseEntity<?> deleteAccount(@RequestBody Map<String, String> body)
    // {
    // // 여기에 코드를 작성하세요.
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Long userId) {

        String password = body.get("password");
        userService.deleteAccount(userId, password);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다");

    }

    /**
     * [7] 설정 조회 — GET /api/users/me/settings
     * 
     * @return UserSettingsDto
     *
     *         로직 힌트:
     *         - 현재 로그인 사용자의 UserSettings 엔티티 조회
     *         - UserSettings → UserSettingsDto 변환 후 반환
     */
    // @GetMapping("/me/settings")
    // public ResponseEntity<?> getSettings() {
    // // 여기에 코드를 작성하세요.
    // }

    /**
     * [8] 설정 수정 — PUT /api/users/me/settings
     * 
     * @param request UserSettingsDto
     *
     *                로직 힌트:
     *                - 현재 사용자의 UserSettings 엔티티 조회 (없으면 새로 생성)
     *                - 필드별 업데이트 → save()
     */
    // @PutMapping("/me/settings")
    // public ResponseEntity<?> updateSettings(@RequestBody UserSettingsDto request)
    // {
    // // 여기에 코드를 작성하세요.
    // }
    
    @GetMapping("/me/settings")
    public ResponseEntity<?> getSettings(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getSettings(userId));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<?> updateSettings(
            @RequestBody UserSettingsDto request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.updateSettings(userId, request));
    }

}
