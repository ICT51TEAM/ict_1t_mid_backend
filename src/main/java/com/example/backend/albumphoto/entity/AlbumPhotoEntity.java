package com.example.backend.albumphoto.entity;

import com.example.backend.album.entity.AlbumEntity;
import com.example.backend.photo.entity.PhotoEntity;

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
- [BACK][DB] ALBUM과 PHOTO의 연결 테이블(ALBUM_PHOTO) 엔티티입니다.

[관련 기능(화면/요청)]
- API: POST /api/albums

[실행 흐름(순서)]
1) AlbumService가 photoIds/slotIndexes를 순회
2) album + photo + slotIndex 조합으로 AlbumPhotoEntity 생성
3) ALBUM_PHOTO 테이블에 저장
*/
@Entity
@Table(name = "ALBUM_PHOTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumPhotoEntity {

    // [DB] ALBUM_PHOTO.ALBUM_PHOTO_ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ALBUM_PHOTO_ID")
    private Long id;

    // [DB] ALBUM_PHOTO.ALBUM_ID (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALBUM_ID", nullable = false)
    private AlbumEntity album;

    // [DB] ALBUM_PHOTO.PHOTO_ID (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PHOTO_ID", nullable = false)
    private PhotoEntity photo;

    // [DB] 배치 순서 (SLOT_INDEX)
    @Column(name = "SLOT_INDEX", nullable = false)
    private Integer slotIndex;
}
