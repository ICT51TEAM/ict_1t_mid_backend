package com.example.backend.auth.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * [이메일 서비스 (Email Service)]
 * - JavaMailSender를 이용해 SMTP 이메일을 발송합니다.
 * - 회원가입 인증 코드, 비밀번호 재설정 코드를 이메일로 보내는 역할
 *
 * [application.yml(또는 application-secrets.yml)에 필요한 SMTP 설정]
 * spring:
 * mail:
 * host: smtp.gmail.com # Gmail SMTP 서버
 * port: 587
 * username: your-email@gmail.com
 * password: your-app-password # Gmail 앱 비밀번호 (2단계 인증 후 생성)
 * properties:
 * mail.smtp.auth: true
 * mail.smtp.starttls.enable: true
 *
 * [필요한 주입 객체]
 * 1. JavaMailSender mailSender : SMTP 이메일 전송 객체 (application.yml 설정 기반 자동 빈 등록)
 *
 * [구현해야 할 메서드]
 *
 * 1. sendVerificationCode(String toEmail, String code)
 * - 회원가입 시 인증번호 발송
 * - MimeMessage 생성 → MimeMessageHelper로 수신자/제목/본문 설정
 * - 예시 코드:
 * MimeMessage message = mailSender.createMimeMessage();
 * MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
 * helper.setTo(toEmail);
 * helper.setSubject("[뉴싸이월드] 이메일 인증 코드");
 * helper.setText("인증 코드: " + code, true); // true = HTML 허용
 * mailSender.send(message);
 *
 * 2. sendPasswordResetCode(String toEmail, String code)
 * - 비밀번호 재설정 인증 코드 발송
 * - 위와 동일한 방식, 제목/본문만 다름
 *
 * 3. generateCode() → String
 * - 6자리 랜덤 숫자 코드 생성
 * - 예: String.format("%06d", new Random().nextInt(1000000))
 * - 반환값: "482917" 같은 6자리 문자열
 *
 * [사용 어노테이션]
 * - @Service : 서비스 빈 등록
 * - @RequiredArgsConstructor : final 필드 생성자 자동 주입
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[ReBuild] 이메일 인증 코드 안내");
        message.setText("인증 코드: " + code + "\n\n5분 안에 입력해주세요.");
        mailSender.send(message);
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[ReBuild] 비밀번호 재설정 코드 안내");
        message.setText("비밀번호 재설정 인증 코드: " + code + "\n\n5분 안에 입력해주세요.");
        mailSender.send(message);
    }

    public String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(1_000_000);
        return String.format("%06d", number);
    }
    // public void sendVerificationCode(String toEmail, String code) {
    // // MimeMessage + MimeMessageHelper 를 사용하여 이메일 발송
    // }

    // public void sendPasswordResetCode(String toEmail, String code) {
    // // 비밀번호 재설정 인증 코드 발송
    // }

    // public String generateCode() {
    // // 6자리 랜덤 코드 생성 후 반환
    // // return String.format("%06d", new Random().nextInt(1000000));
    // }
}
