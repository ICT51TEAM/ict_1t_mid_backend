package com.example.backend.global.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
[파일 역할]
- [BACK][API] 서버 에러 응답 구조를 전역으로 통일하는 DTO입니다.

[관련 기능(화면/요청)]
- 사진 업로드 API, 앨범 생성 API 등에서 검증 실패/예외 응답에 공통 사용합니다.

[실행 흐름(순서)]
1) Controller/Service에서 오류 상황 감지
2) code/message/details 값을 ApiErrorResponse에 담기
3) ResponseEntity로 프론트에 반환
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    // [BACK][API] 에러 코드 (예: VALIDATION_ERROR)
    private String code;

    // [BACK][API] 사용자에게 보여줄 메시지
    private String message;

    // [BACK][API] 추가 디버깅 정보 (필드명, 입력값 등)
    private Map<String, Object> details;
}
