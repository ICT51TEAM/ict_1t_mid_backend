package com.example.backend.fss.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.example.backend.fss.service.FssService;

import lombok.RequiredArgsConstructor;

/**
 * [금융감독원 API 프록시 컨트롤러 (FSS Controller)]
 * - 프론트엔드 fssService.js에서 금융감독원 공시 데이터를 조회할 때
 * 직접 외부 API를 호출하지 않고, 백엔드를 프록시로 경유합니다.
 *
 * [왜 프록시가 필요한가?]
 * - 프론트엔드에서 직접 외부 API를 호출하면 CORS 에러 발생
 * - API 키가 프론트엔드 코드에 노출되는 보안 문제
 * - 백엔드에서 API 키를 안전하게 관리하고, 데이터를 가공하여 전달
 *
 * [프론트엔드 fssService.js와의 매핑]
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 프론트 함수 │ 백엔드 엔드포인트 │
 * ├─────────────────────────────────┼─────────────────────────────┤
 * │ fssService.fetchFssData(start, end) │ GET /api/fss/list │
 * └─────────────────────────────────┴─────────────────────────────┘
 *
 * [필요한 주입 서비스]
 * - FssService fssService
 */
@RestController
@RequestMapping("/fss")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FssController implements FssControllerDocs {

    private final FssService fssService;

    /**
     * [1] 금융감독원 공시 데이터 조회 — GET /api/fss/list
     * 
     * @param startDate 조회 시작일 (YYYYMMDD 형식, 예: "20260101")
     * @param endDate   조회 종료일 (YYYYMMDD 형식, 예: "20260223")
     * @return List<FssResponseDto> 또는 원본 JSON 배열
     *
     *         로직 힌트:
     *         - fssService.fetchFssData(startDate, endDate) 호출
     *         - 내부에서 금감원 OpenAPI에 HTTP 요청 → 응답 데이터 파싱 → 반환
     *
     *         [금감원 API URL 예시]
     *         http://fss.or.kr/fss/kr/openApi/api/v1/disclosure?apiType=json
     *         &startDate=20260101&endDate=20260223&apiKey={FSS_API_KEY}
     */

    // // 여기에 코드를 작성하세요

    @GetMapping("/list")
    public Object getFss(
            @RequestParam(value = "apiType", defaultValue = "json") String apiType,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        return fssService.getFssDataFromOpenApi(apiType, startDate, endDate);
    }
}
