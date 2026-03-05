package com.example.backend.fss.controller;

import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "금융감독원 API 프록시", description = "금융감독원 공시 데이터 연동 관련 프록시 API입니다.")
public interface FssControllerDocs {

    @Operation(summary = "금융감독원 공시 데이터 조회", description = "금융감독원 OpenAPI를 호출하여 공시 데이터를 프록시 형태로 가져옵니다.")
    Object getFss(
            @RequestParam(value = "apiType", defaultValue = "json") String apiType,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);
}
