package com.example.backend.badge.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * [달개 유형 엔티티 (BadgeType Entity)]
 * - 달개의 종류를 정의하는 마스터 테이블
 * - 오라클 DB의 BADGE_TYPES 테이블과 매핑
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Sequence
 * 2. name (String) : 달개 유형 이름 (예: "좋아요", "슬퍼요", "화나요", "응원해요")
 * - @Column(nullable = false, unique = true)
 * 3. emoji (String) : 달개 이모지 (예: "❤️", "😢", "😠", "💪")
 * - 프론트엔드에서 달개 아이콘 표시에 사용
 * 4. sortOrder (int) : 정렬 순서 (프론트에서 달개 선택 UI 순서)
 *
 * [프론트엔드 연동]
 * - badgeService.getAllTypes() → GET /api/badges/types
 * → 이 테이블의 전체 레코드를 조회하여 반환
 *
 * [초기 데이터]
 * 프로젝트 배포 시 아래와 같은 초기 데이터를 INSERT 해야 합니다:
 * INSERT INTO BADGE_TYPES (ID, NAME, EMOJI, SORT_ORDER) VALUES (1, '좋아요', '❤️',
 * 1);
 * INSERT INTO BADGE_TYPES (ID, NAME, EMOJI, SORT_ORDER) VALUES (2, '슬퍼요', '😢',
 * 2);
 * INSERT INTO BADGE_TYPES (ID, NAME, EMOJI, SORT_ORDER) VALUES (3, '화나요', '😠',
 * 3);
 * INSERT INTO BADGE_TYPES (ID, NAME, EMOJI, SORT_ORDER) VALUES (4, '응원해요',
 * '💪', 4);
 */
@Entity
@Table(name = "BADGE_TYPES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "badge_type_seq")
    @SequenceGenerator(name = "badge_type_seq", sequenceName = "BADGE_TYPE_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME",nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "EMOJI", length = 10)
    private String emoji;
    
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "SORT_ORDER", nullable = false )
    private int sortOrder;
}
