package com.example.backend.albumtag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.albumtag.entity.AlbumTagEntity;

/*
[파일 역할]
- [BACK][DB] ALBUM_TAGS 테이블 CRUD Repository입니다.

[관련 기능(화면/요청)]
- AlbumService.saveAlbumTags()에서 앨범-태그 연결 저장 시 사용합니다.
- AlbumService.getAlbumDetail()에서 태그 목록 조회 시 사용합니다.
*/
public interface AlbumTagRepository extends JpaRepository<AlbumTagEntity, Long> {

    // [DB 조회]
    // - 어디서 호출? : AlbumService.getAlbumDetail()
    // - 입력값 : albumId
    // - 출력값 : albumTagId 오름차순 태그 연결 목록
    List<AlbumTagEntity> findByAlbum_IdOrderByIdAsc(Long albumId);

    // [DB 삭제]
    // - 어디서 호출? : AlbumService.deleteAlbum()
    // - 입력값 : albumId
    // - 출력값 : 없음(해당 앨범의 태그 연결 데이터 삭제)
    void deleteByAlbum_Id(Long albumId);
}
