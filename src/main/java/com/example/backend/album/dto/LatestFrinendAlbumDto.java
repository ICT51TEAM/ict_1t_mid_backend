package com.example.backend.album.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // JSON 변환을 위해 기본 생성자 추가 권장
@Builder
public class LatestFrinendAlbumDto {
    private Long postId;
    private Long albumId;      // 추가: 상세 페이지 이동을 위한 ID
    private String title;      
    private String author;     
    private String authorBadge; 
    private String date;
}
