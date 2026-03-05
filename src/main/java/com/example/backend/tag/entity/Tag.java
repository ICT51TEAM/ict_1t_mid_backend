package com.example.backend.tag.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * [태그 엔티티 (Tag Entity)]
 * - 앨범에 붙이는 태그를 저장하는 테이블
 * - 오라클 DB의 TAGS 테이블과 매핑
 *
 * [필요한 변수]
 * 1. id (Long) : PK, Oracle Identity Column -> @Column(name = "TAG_ID")
 * 2. name (String) : 태그 이름 (예: "여행", "맛집", "일상")
 *    - @Column(name = "TAG_NAME", nullable = false, unique = true)
 * 3. usageCount (Integer) : 사용 횟수 -> @Column(name = "USAGE_COUNT")
 *
 * [앨범과의 관계]
 * - Album ↔ Tag 는 N:M (다대다) 관계
 * - 별도 중간 엔티티 AlbumTagEntity 로 연결
 */
@Entity
@Table(name = "TAGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TAG_ID")
    private Long id;

    @Column(name = "TAG_NAME", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "USAGE_COUNT", nullable = false)
    private Integer usageCount = 0;
}
