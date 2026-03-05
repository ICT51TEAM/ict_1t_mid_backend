package com.example.backend.albumtag.entity;

import com.example.backend.album.entity.AlbumEntity;
import com.example.backend.tag.entity.Tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
[파일 역할]
- [BACK][DB] ALBUM과 TAG의 연결 테이블(ALBUM_TAGS) 엔티티입니다.

[관련 기능(화면/요청)]
- API: POST /api/albums (tags 필드)

[실행 흐름(순서)]
1) AlbumService가 태그 문자열 목록 정규화
2) TAG 조회/생성 후 AlbumTagEntity 생성
3) ALBUM_TAGS 테이블에 저장
*/
@Entity
@Table(name = "ALBUM_TAGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumTagEntity {

    // [DB] ALBUM_TAGS.ALBUM_TAG_ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ALBUM_TAG_ID")
    private Long id;

    // [DB] ALBUM_TAGS.ALBUM_ID (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALBUM_ID", nullable = false)
    private AlbumEntity album;

    // [DB] ALBUM_TAGS.TAG_ID (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAG_ID", nullable = false)
    private Tag tag;
}
