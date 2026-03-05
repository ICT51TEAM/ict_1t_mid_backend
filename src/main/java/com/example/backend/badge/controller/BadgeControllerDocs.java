package com.example.backend.badge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.backend.badge.dto.BadgeStatsDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "달개(뱃지) API", description = "달개 통계 및 랭킹 관련 데이터 조회 API입니다.")
public interface BadgeControllerDocs {

    @Operation(summary = "나의 달개 통계 조회", description = "내가 부여받은 달개의 총 개수와 달개 종류별 개수를 조회합니다.")
    ResponseEntity<BadgeStatsDto> getMyBadgeStats(Authentication authentication);

    @Operation(summary = "친구의 달개 통계 조회", description = "친구가 부여받은 달개의 총 개수와 달개 종류별 개수를 조회합니다.")
    ResponseEntity<BadgeStatsDto> getFriendsBadgeStats(@PathVariable Long userId);
}
