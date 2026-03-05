package com.example.backend.fss.dto;

import lombok.*;

/**
 * [금감원 공시 응답 DTO (FSS Response)]
 * - 금감원 API에서 받아온 공시 데이터를 프론트엔드에 전달하는 DTO
 * - 실제 응답 구조에 맞춰 필드를 조정하세요
 *
 * [필요한 변수 (금감원 API 응답 기준 — 실제 필드명은 API 문서 확인 필요)]
 * 1. companyName (String) : 회사명
 * 2. reportName (String) : 보고서명 (공시 제목)
 * 3. reportDate (String) : 접수일자
 * 4. reportUrl (String) : 보고서 상세 URL
 * 5. market (String) : 시장구분 (코스피/코스닥)
 *
 * [프론트엔드 사용 위치]
 * - FssPage에서 공시 목록 테이블 렌더링
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FssResponseDto {
    private String companyName;
    private String reportName;
    private String reportDate;
    private String reportUrl;
    private String market;
}
