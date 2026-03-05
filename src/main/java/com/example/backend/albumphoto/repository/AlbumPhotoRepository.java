package com.example.backend.albumphoto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.albumphoto.entity.AlbumPhotoEntity;

/*
[파일 역할]
- [BACK][DB] ALBUM_PHOTO 테이블 CRUD Repository입니다.

[관련 기능(화면/요청)]
- AlbumService.saveAlbumPhotos()에서 연결 데이터 저장 시 사용합니다.
- AlbumService.getAlbumDetail()에서 슬롯 순서대로 사진 조회 시 사용합니다.
- AlbumService.getAlbumFeed()에서 대표 사진(첫 슬롯) 조회 시 사용합니다.
*/
public interface AlbumPhotoRepository extends JpaRepository<AlbumPhotoEntity, Long> {

    // [DB 조회]
    // - 어디서 호출? : AlbumService.getAlbumDetail()
    // - 입력값 : albumId
    // - 출력값 : slotIndex 오름차순 사진 연결 목록
    List<AlbumPhotoEntity> findByAlbum_IdOrderBySlotIndexAsc(Long albumId);

    // [DB 조회]
    // - 어디서 호출? : AlbumService.getAlbumFeed()
    // - 입력값 : albumId
    // - 출력값 : slotIndex 기준 대표 사진 1건
    Optional<AlbumPhotoEntity> findFirstByAlbum_IdOrderBySlotIndexAsc(Long albumId);

    // [DB 삭제]
    // - 어디서 호출? : AlbumService.deleteAlbum()
    // - 입력값 : albumId
    // - 출력값 : 없음(해당 앨범의 사진 연결 데이터 삭제)
    void deleteByAlbum_Id(Long albumId);
}
