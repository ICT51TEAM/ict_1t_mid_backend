package com.example.backend.badge.dto;

import lombok.*;

/**
 * [앨범 달개 정보 DTO]
 * - 특정 앨범에 달린 달개(이모지 반응)의 집계 정보를 프론트엔드에 전달
 * - 이모지, 이름, 개수를 담는다
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDalgaeDto {
    private String emoji;
    private String name;
    private long count;
}
